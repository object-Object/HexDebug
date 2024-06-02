package gay.`object`.hexdebug.blocks.splicing

import gay.`object`.hexdebug.networking.msg.MsgSplicingTableActionC2S
import gay.`object`.hexdebug.splicing.ISplicingTable
import gay.`object`.hexdebug.splicing.SplicingTableAction
import gay.`object`.hexdebug.splicing.Selection
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.world.SimpleContainer

@Environment(EnvType.CLIENT)
class ClientSplicingTableContainer : SimpleContainer(ISplicingTable.CONTAINER_SIZE), ISplicingTable {
    override fun getClientView() = null

    /** Called on the client. */
    override fun runAction(action: SplicingTableAction, selection: Selection?) = selection.also {
        MsgSplicingTableActionC2S(action, it).sendToServer()
    }
}
