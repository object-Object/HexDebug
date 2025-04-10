package gay.`object`.hexdebug.blocks.splicing

enum class SplicingTableDataSlot {
    // media is a long but ContainerData stores ints :/
    // and also DataSlot serializes ints as shorts for some reason
    MEDIA_0,
    MEDIA_1,
    MEDIA_2,
    MEDIA_3;

    val index = ordinal

    companion object {
        val size = entries.size
    }
}
