package gay.`object`.hexdebug.blocks.splicing

import net.minecraft.network.FriendlyByteBuf
import kotlin.math.max

typealias Selection = SelectionType<*>

/**
 * A selection range. `start` is inclusive, `end` is exclusive.
 */
sealed class SelectionType<Self : SelectionType<Self>> private constructor(val start: Int, val end: Int) {
    val size = max(0, end - start)
    val lastIndex = max(start, end - 1)

    abstract fun copy(start: Int, end: Int): Self?

    fun copy(start: Int? = null, end: Int? = null): Self? = copy(start ?: this.start, end ?: this.end)

    fun expandRight(extra: Int): Range? = Range.of(start, end + extra)

    fun nudge(delta: Int): Self? = copy(start + delta, end + delta)

    fun toPair() = Pair(start, end)

    class Edge private constructor(val index: Int) : SelectionType<Edge>(index, index) {
        private val isValid = index >= 0

        override fun copy(start: Int, end: Int) = of(start)

        companion object {
            fun of(index: Int): Edge? = Edge(index).takeIf { it.isValid }
        }
    }

    class Range private constructor(start: Int, end: Int) : SelectionType<Range>(start, end) {
        @Suppress("ConvertTwoComparisonsToRangeCheck")
        private val isValid = start >= 0 && end > start

        override fun copy(start: Int, end: Int) = of(start, end)

        companion object {
            fun of(start: Int, end: Int): Range? = Range(start, end).takeIf { it.isValid }
        }
    }

    companion object {
        fun of(start: Int, end: Int = -1): Selection? = Range.of(start, end) ?: Edge.of(start)

        fun withSize(start: Int, size: Int): Selection? = of(start, start + size)
    }
}

/** Writes a [Selection] to the buffer. `null` is encoded as `Selection(-1, -1)`. */
fun FriendlyByteBuf.writeSelection(selection: Selection?) {
    val (start, end) = selection?.toPair() ?: Pair(-1, -1)
    writeInt(start)
    writeInt(end)
}

/** Reads a [Selection] from the buffer. `null` is encoded as `Selection(-1, -1)`. */
fun FriendlyByteBuf.readSelection(): Selection? {
    return Selection.of(
        start = readInt(),
        end = readInt(),
    )
}
