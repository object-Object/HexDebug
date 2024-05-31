package gay.`object`.hexdebug.blocks.splicing

import gay.`object`.hexdebug.blocks.base.ContainerSlotDelegate
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack

interface ISplicingTable : Container {
    var iotaHolder: ItemStack
    var clipboard: ItemStack

    fun iotaHolderDelegate() = ContainerSlotDelegate(IOTA_HOLDER_INDEX)
    fun clipboardDelegate() = ContainerSlotDelegate(CLIPBOARD_INDEX)

    fun runAction(action: Action, selection: Selection)

    companion object {
        const val CONTAINER_SIZE = 2
        const val IOTA_HOLDER_INDEX = 0
        const val CLIPBOARD_INDEX = 1
    }

    enum class Action {
        NUDGE_LEFT,
        NUDGE_RIGHT,
        DUPLICATE,
        DELETE,
        UNDO,
        REDO,
        CUT,
        COPY,
        PASTE,
        PASTE_SPLAT,
    }

    data class Selection(val start: Int, val end: Int)
}
