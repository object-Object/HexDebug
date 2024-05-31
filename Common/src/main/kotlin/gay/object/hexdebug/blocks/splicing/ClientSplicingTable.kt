package gay.`object`.hexdebug.blocks.splicing

import gay.`object`.hexdebug.blocks.splicing.ISplicingTable.Action
import gay.`object`.hexdebug.blocks.splicing.ISplicingTable.Selection
import gay.`object`.hexdebug.networking.msg.MsgSplicingTableActionC2S
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.world.SimpleContainer

@Environment(EnvType.CLIENT)
class ClientSplicingTable : SimpleContainer(ISplicingTable.CONTAINER_SIZE), ISplicingTable {
    override var iotaHolder by iotaHolderDelegate()
    override var clipboard by clipboardDelegate()

    /** Called on the client. */
    override fun runAction(action: Action, selection: Selection) = selection.also {
        MsgSplicingTableActionC2S(action, it).sendToServer()
    }
}
