package gay.`object`.hexdebug.splicing

import gay.`object`.hexdebug.blocks.base.ContainerDataDelegate
import net.minecraft.world.inventory.ContainerData

enum class SplicingTableDataSlot {
    // media is a long but ContainerData stores ints :/
    MEDIA_LOW,
    MEDIA_HIGH;

    val index = ordinal

    fun delegate(data: ContainerData) = ContainerDataDelegate(data, index)

    companion object {
        val size = entries.size
    }
}
