package gay.`object`.hexdebug.blocks.splicing

import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.math.HexPattern
import gay.`object`.hexdebug.networking.msg.MsgSplicingTableActionC2S
import gay.`object`.hexdebug.networking.msg.MsgSplicingTableNewStaffPatternC2S
import gay.`object`.hexdebug.splicing.ISplicingTable
import gay.`object`.hexdebug.splicing.Selection
import gay.`object`.hexdebug.splicing.SplicingTableAction
import gay.`object`.hexdebug.splicing.SplicingTableSlot
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack

class ClientSplicingTableContainer : SimpleContainer(SplicingTableSlot.container_size), ISplicingTable {
    override fun getClientView() = null
    override fun listStackChanged(stack: ItemStack) {}
    override fun clipboardStackChanged(stack: ItemStack) {}

    /** Called on the client. */
    override fun runAction(action: SplicingTableAction, player: ServerPlayer?, selection: Selection?) = selection.also {
        MsgSplicingTableActionC2S(action, it).sendToServer()
    }

    override fun drawPattern(player: ServerPlayer?, pattern: HexPattern, index: Int, selection: Selection?): Pair<Selection?, ResolvedPatternType> {
        MsgSplicingTableNewStaffPatternC2S(pattern, index, selection).sendToServer()
        return selection to ResolvedPatternType.UNRESOLVED
    }
}
