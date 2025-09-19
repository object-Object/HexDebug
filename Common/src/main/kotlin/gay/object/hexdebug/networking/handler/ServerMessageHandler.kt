package gay.`object`.hexdebug.networking.handler

import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.adapter.DebugAdapterManager
import gay.`object`.hexdebug.gui.splicing.SplicingTableMenu
import gay.`object`.hexdebug.networking.msg.*
import net.minecraft.server.level.ServerPlayer

fun HexDebugMessageC2S.applyOnServer(ctx: PacketContext) = ctx.queue {
    val player = ctx.player as ServerPlayer
    when (this) {
        is MsgDebugAdapterProxy -> {
            DebugAdapterManager[ctx.player]?.launcher?.handleMessage(content)
        }

        is MsgSplicingTableActionC2S -> {
            val menu = SplicingTableMenu.getInstance(ctx.player) ?: return@queue
            menu.table.runAction(action, ctx.player as? ServerPlayer)
        }

        is MsgSplicingTableGetDataC2S -> {
            SplicingTableMenu.getInstance(ctx.player)?.sendData(ctx.player as ServerPlayer)
        }

        is MsgSplicingTableNewStaffPatternC2S -> {
            val menu = SplicingTableMenu.getInstance(player) ?: return@queue
            val resolutionType = menu.table.drawPattern(player, pattern, index)
            MsgSplicingTableNewStaffPatternS2C(resolutionType, index).sendToPlayer(player)
        }

        is MsgSplicingTableSelectIndexC2S -> {
            val menu = SplicingTableMenu.getInstance(ctx.player) ?: return@queue
            menu.table.selectIndex(
                player = ctx.player as? ServerPlayer,
                index = index,
                hasShiftDown = hasShiftDown,
                isIota = isIota,
            )
        }
    }
}
