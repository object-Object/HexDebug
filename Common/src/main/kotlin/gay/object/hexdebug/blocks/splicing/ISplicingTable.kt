package gay.`object`.hexdebug.blocks.splicing

import gay.`object`.hexdebug.blocks.base.ContainerSlotDelegate
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack

interface ISplicingTable : Container {
    var iotaHolder: ItemStack
    var clipboard: ItemStack

    fun iotaHolderDelegate() = ContainerSlotDelegate(0)
    fun clipboardDelegate() = ContainerSlotDelegate(1)

    companion object {
        const val CONTAINER_SIZE = 2
    }
}
