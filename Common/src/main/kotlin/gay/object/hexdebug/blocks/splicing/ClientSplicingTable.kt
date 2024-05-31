package gay.`object`.hexdebug.blocks.splicing

import gay.`object`.hexdebug.blocks.splicing.ISplicingTable.Action
import gay.`object`.hexdebug.blocks.splicing.ISplicingTable.Selection
import gay.`object`.hexdebug.networking.msg.MsgSplicingTableActionC2S
import net.minecraft.world.SimpleContainer

class ClientSplicingTable : SimpleContainer(ISplicingTable.CONTAINER_SIZE), ISplicingTable {
    override var iotaHolder by iotaHolderDelegate()
    override var clipboard by clipboardDelegate()

    override fun runAction(action: Action, selection: Selection) {
        MsgSplicingTableActionC2S(action, selection).sendToServer()
    }
}
