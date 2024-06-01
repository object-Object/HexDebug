package gay.`object`.hexdebug.blocks.splicing

import net.minecraft.network.FriendlyByteBuf
import kotlin.math.max

/**
 * An inclusive selection range.
 */
sealed class Selection private constructor(val start: Int, val end: Int) {
    val size = max(0, end - start + 1)
    val lastIndex = max(start, end)

    val range = start..end

    abstract fun copy(start: Int, end: Int): Selection?

    fun copy(start: Int? = null, end: Int? = null) = copy(start ?: this.start, end ?: this.end)

    fun expandRight(extra: Int) = Range.of(start, end + extra)

    fun nudge(delta: Int) = copy(start + delta, end + delta)

    fun toPair() = Pair(start, end)

    class Edge private constructor(val index: Int) : Selection(index, index - 1) {
        private val isValid = index >= 0

        override fun copy(start: Int, end: Int) = of(start)

        companion object {
            fun of(index: Int): Edge? = Edge(index).takeIf { it.isValid }
        }
    }

    class Range private constructor(start: Int, end: Int) : Selection(start, end) {
        @Suppress("ConvertTwoComparisonsToRangeCheck")
        private val isValid = start >= 0 && end >= start

        override fun copy(start: Int, end: Int) = of(start, end)

        companion object {
            fun of(start: Int, end: Int): Range? = Range(start, end).takeIf { it.isValid }
        }
    }

    companion object {
        fun withSize(start: Int, size: Int) = rangeOrEdge(start, start + size - 1)

        fun rangeOrEdge(start: Int, end: Int) = Range.of(start, end) ?: Edge.of(start)
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
    return Selection.rangeOrEdge(
        start = readInt(),
        end = readInt(),
    )
}
