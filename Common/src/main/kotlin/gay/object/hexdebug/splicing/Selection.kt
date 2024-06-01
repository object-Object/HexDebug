package gay.`object`.hexdebug.splicing

import net.minecraft.network.FriendlyByteBuf
import kotlin.math.max
import kotlin.math.min

/**
 * An inclusive selection range.
 */
class Selection private constructor(val from: Int, val to: Int?) {
    val start = to?.let { min(from, it) } ?: from
    val end = to?.let { max(from, it) }

    private val isValid = start >= 0 && (end == null || end >= start)

    val isEdge = end == null
    val isRange = !isEdge

    val lastIndex = end ?: start

    val size = end?.let { it - start + 1 } ?: 0

    val range = start..(end ?: -1)

    fun moveBy(delta: Int) = of(start + delta, end?.plus(delta))

    fun expandRight(extra: Int) = of(start, (end ?: (start - 1)) + extra)

    companion object {
        fun withSize(from: Int, size: Int) = range(from, from + size - 1)

        fun range(from: Int, to: Int) = of(from, to)

        fun edge(index: Int) = of(index, null)

        fun of(from: Int, to: Int?) = Selection(from, to).takeIf { it.isValid }
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
