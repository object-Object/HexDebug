package gay.`object`.hexdebug.splicing

import net.minecraft.network.FriendlyByteBuf
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * An inclusive selection range.
 */
sealed class Selection private constructor(val from: Int, open val to: Int?) {
    abstract val start: Int
    abstract val end: Int?
    /** For ranges, the end index. For edges, the index of the iota to the left. */
    abstract val lastIndex: Int

    protected val isValid by lazy { start >= 0 && (end?.let { it >= start } ?: true) }

    val size by lazy { end?.let { it - start + 1 } ?: 0 }

    abstract operator fun contains(value: Number): Boolean

    /* Expand the selection. A positive value expands to the right, while a negative value expands to the left. */
    abstract fun expandBy(extra: Int): Selection?

    abstract fun moveBy(delta: Int): Selection?

    fun <T> subList(list: List<T>) = list.subList(start, end?.plus(1) ?: start).toList()

    fun <T> mutableSubList(list: MutableList<T>) = list.subList(start, end?.plus(1) ?: start)

    override fun equals(other: Any?): Boolean {
        return other is Selection && from == other.from && to == other.to
    }

    override fun hashCode(): Int {
        return Objects.hash(from, to)
    }

    class Range private constructor(from: Int, override val to: Int) : Selection(from, to) {
        override val start = min(from, to)
        override val end = max(from, to)
        override val lastIndex = end

        private val range = start..end
        override fun contains(value: Number) = value in range

        override fun expandBy(extra: Int) = of(from, to + extra)

        override fun moveBy(delta: Int) = of(from + delta, to + delta)

        companion object {
            fun of(from: Int, to: Int) = Range(from, to).takeIf { it.isValid }
        }
    }

    /** Note: index refers to the index of the cell to the right of this edge. */
    class Edge private constructor(val index: Int) : Selection(index, null) {
        override val to = null
        override val start = index
        override val end = null
        override val lastIndex = index - 1

        override fun contains(value: Number) = false

        override fun expandBy(extra: Int) = when {
            // eg. index = 10, extra = 1 -> range(10, 10)
            extra > 0 -> range(index, index + extra - 1)
            // eg. index = 10, extra = -1 -> range(9, 9)
            extra < 0 -> range(index - 1, index + extra)
            else -> this
        }

        override fun moveBy(delta: Int) = of(index + delta)

        companion object {
            fun of(index: Int) = Edge(index).takeIf { it.isValid }
        }
    }

    companion object {
        fun withSize(from: Int, size: Int) = range(from, from + size - 1)

        fun of(from: Int, to: Int?) = if (to != null) range(from, to) else edge(from)

        fun range(from: Int, to: Int) = Range.of(from, to)

        fun edge(index: Int) = Edge.of(index)

        fun fromRawIndices(from: Int, to: Int) = when {
            from < 0 -> null
            to < 0 -> edge(from)
            else -> range(from, to)
        }
    }
}

/** Writes an optional [Selection] to the buffer. */
fun FriendlyByteBuf.writeSelection(selection: Selection?) {
    writeNullable(selection) { buf, value ->
        value.also {
            buf.writeInt(it.from)
            buf.writeNullable(it.to, FriendlyByteBuf::writeInt)
        }
    }
}

/** Reads an optional [Selection] from the buffer. */
fun FriendlyByteBuf.readSelection(): Selection? {
    return readNullable { buf ->
        Selection.of(
            from = buf.readInt(),
            to = buf.readNullable(FriendlyByteBuf::readInt),
        )
    }
}
