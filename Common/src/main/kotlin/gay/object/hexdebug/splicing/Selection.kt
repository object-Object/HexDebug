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

    protected val isValid by lazy { start >= 0 && (end?.let { it >= start } ?: true) }

    val lastIndex by lazy { end ?: start }

    val size by lazy { end?.let { it - start + 1 } ?: 0 }

    abstract operator fun contains(value: Number): Boolean

    abstract fun expandRight(extra: Int): Selection?

    fun moveBy(delta: Int) = of(start + delta, end?.plus(delta))

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

        private val range = start..end
        override fun contains(value: Number) = value in range

        override fun expandRight(extra: Int) = if (from <= to) {
            of(from, to + extra)
        } else {
            of(from + extra, to)
        }

        companion object {
            fun of(from: Int, to: Int) = Range(from, to).takeIf { it.isValid }
        }
    }

    /** Note: index refers to the index of the cell to the right of this edge. */
    class Edge private constructor(val index: Int) : Selection(index, null) {
        override val to = null
        override val start = index
        override val end = null

        override fun contains(value: Number) = false

        override fun expandRight(extra: Int) = when {
            extra > 0 -> range(index, index + extra)
            extra < 0 -> range(index - 1, index + extra)
            else -> this
        }

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
