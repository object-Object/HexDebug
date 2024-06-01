package gay.`object`.hexdebug.blocks.splicing

import gay.`object`.hexdebug.blocks.splicing.Selection.Edge
import net.minecraft.network.FriendlyByteBuf
import kotlin.math.max

/**
 * An inclusive selection range.
 *
 * Note: for [Edge], `end == start - 1`.
 */
sealed class Selection private constructor(val start: Int, val end: Int) {
    val lastIndex = max(start, end)
    val size = max(0, end - start + 1)
    val range = start..end

    abstract fun copy(start: Int, end: Int): Selection?

    fun copy(start: Int? = null, end: Int? = null) = copy(start ?: this.start, end ?: this.end)

    fun expandRight(extra: Int) = Range.of(start, end + extra)

    fun moveBy(delta: Int) = copy(start + delta, end + delta)

    class Edge private constructor(val index: Int) : Selection(index, index - 1) {
        override fun copy(start: Int, end: Int) = of(start)

        companion object {
            fun of(index: Int) = Edge(index).takeIf { index >= 0 }
        }
    }

    class Range private constructor(start: Int, end: Int) : Selection(start, end) {
        override fun copy(start: Int, end: Int) = of(start, end)

        companion object {
            @Suppress("ConvertTwoComparisonsToRangeCheck")
            fun of(start: Int, end: Int) = Range(start, end).takeIf { start >= 0 && end >= start }
        }
    }

    companion object {
        fun withSize(start: Int, size: Int) = rangeOrEdge(start, start + size - 1)

        fun rangeOrEdge(start: Int, end: Int) = Range.of(start, end) ?: Edge.of(start)
    }
}

/** Writes a [Selection] to the buffer. `null` is encoded as `Selection(-1, -1)`. */
fun FriendlyByteBuf.writeSelection(selection: Selection?) {
    selection.also {
        writeInt(it?.start ?: -1)
        writeInt(it?.end ?: -1)
    }
}

/** Reads a [Selection] from the buffer. `null` is encoded as `Selection(-1, -1)`. */
fun FriendlyByteBuf.readSelection(): Selection? {
    return Selection.rangeOrEdge(
        start = readInt(),
        end = readInt(),
    )
}
