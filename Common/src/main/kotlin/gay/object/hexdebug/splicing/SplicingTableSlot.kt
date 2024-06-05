package gay.`object`.hexdebug.splicing

import gay.`object`.hexdebug.blocks.base.ContainerSlotDelegate

// only slightly scuffed
private var nextSlotIndex = 0

/** Iterator values: (index, x, y) */
enum class SplicingTableSlot(val width: Int, val height: Int) : Iterable<Triple<Int, Int, Int>> {
    LIST,
    CLIPBOARD,
    MEDIA,
    STAFF,
    STORAGE(2, 3);

    constructor() : this(1, 1)

    val size = width * height

    val index = nextSlotIndex
    init {
        require(size >= 1)
        nextSlotIndex += size
    }

    val lastIndex = index + size - 1

    val delegates = (index..lastIndex).map { ContainerSlotDelegate(it) }

    /** A [ContainerSlotDelegate] for the *first* slot in this range. */
    val delegate = delegates[0]

    /** Values: (index, x, y) */
    override fun iterator() = iterator {
        for (y in 0 until height) {
            for (x in 0 until width) {
                yield(Triple(index + x + y * width, x, y))
            }
        }
    }

    override fun toString() = name + if (size == 1) "($index)" else "($index..$lastIndex)"

    companion object {
        val container_size = entries.last().lastIndex + 1
    }
}
