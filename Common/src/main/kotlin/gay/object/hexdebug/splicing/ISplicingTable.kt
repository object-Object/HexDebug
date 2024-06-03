package gay.`object`.hexdebug.splicing

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack

interface ISplicingTable : Container {
    fun getClientView(): SplicingTableClientView?

    fun listStackChanged(stack: ItemStack)

    fun clipboardStackChanged(stack: ItemStack)

    /** Runs the given action, and returns an updated selection (or null if the selection should be removed). */
    fun runAction(action: SplicingTableAction, player: ServerPlayer?, selection: Selection?): Selection?

    companion object {
        const val CONTAINER_SIZE = 2
        const val LIST_INDEX = 0
        const val CLIPBOARD_INDEX = 1
    }
}
