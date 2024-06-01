package gay.`object`.hexdebug.splicing

import net.minecraft.network.FriendlyByteBuf

/**
 * An inclusive selection range.
 */
class Selection private constructor(val start: Int, val end: Int?) {
    private val isValid = start >= 0 && (end == null || end >= start)

    val isEdge = end == null
    val isRange = !isEdge

    val lastIndex = end ?: start

    val size = end?.let { it - start + 1 } ?: 0

    val range = start..(end ?: -1)

    fun moveBy(delta: Int) = of(start + delta, end?.plus(delta))

    fun expandRight(extra: Int) = of(start, (end ?: (start - 1)) + extra)

    companion object {
        fun withSize(start: Int, size: Int) = range(start, start + size - 1)

        fun range(start: Int, end: Int) = of(start, end)

        fun edge(index: Int) = of(index, null)

        fun of(start: Int, end: Int?) = Selection(start, end?.takeIf { it >= start }).takeIf { it.isValid }
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
    return Selection.of(
        start = readInt(),
        end = readInt(),
    )
}
