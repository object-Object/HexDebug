package gay.`object`.hexdebug.blocks.splicing

import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.math.HexPattern
import gay.`object`.hexdebug.networking.msg.MsgSplicingTableActionC2S
import gay.`object`.hexdebug.networking.msg.MsgSplicingTableNewStaffPatternC2S
import gay.`object`.hexdebug.networking.msg.MsgSplicingTableSelectIndexC2S
import gay.`object`.hexdebug.splicing.ISplicingTable
import gay.`object`.hexdebug.splicing.SplicingTableAction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.SimpleContainer

class ClientSplicingTableContainer : SimpleContainer(SplicingTableItemSlot.container_size), ISplicingTable {
    override fun getClientView() = null

    /** Called on the client. */
    override fun runAction(action: SplicingTableAction, player: ServerPlayer?) {
        MsgSplicingTableActionC2S(action).sendToServer()
    }

    override fun drawPattern(player: ServerPlayer?, pattern: HexPattern, index: Int): ResolvedPatternType {
        MsgSplicingTableNewStaffPatternC2S(pattern, index).sendToServer()
        return ResolvedPatternType.UNRESOLVED
    }

    override fun selectIndex(player: ServerPlayer?, index: Int, hasShiftDown: Boolean, isIota: Boolean) {
        MsgSplicingTableSelectIndexC2S(index, hasShiftDown = hasShiftDown, isIota = isIota).sendToServer()
    }
}
