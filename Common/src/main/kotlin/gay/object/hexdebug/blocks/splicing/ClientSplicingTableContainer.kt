package gay.`object`.hexdebug.blocks.splicing

import gay.`object`.hexdebug.networking.msg.MsgSplicingTableActionC2S
import gay.`object`.hexdebug.splicing.ISplicingTable
import gay.`object`.hexdebug.splicing.Selection
import gay.`object`.hexdebug.splicing.SplicingTableAction
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack

@Environment(EnvType.CLIENT)
class ClientSplicingTableContainer : SimpleContainer(ISplicingTable.CONTAINER_SIZE), ISplicingTable {
    override fun getClientView() = null
    override fun listStackChanged(stack: ItemStack) {}
    override fun clipboardStackChanged(stack: ItemStack) {}

    /** Called on the client. */
    override fun runAction(action: SplicingTableAction, player: ServerPlayer?, selection: Selection?) = selection.also {
        MsgSplicingTableActionC2S(action, it).sendToServer()
    }
}
