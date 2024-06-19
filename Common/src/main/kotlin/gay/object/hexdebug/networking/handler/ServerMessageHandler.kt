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
            val newSelection = menu.table.runAction(action, ctx.player as? ServerPlayer, selection)
            MsgSplicingTableNewSelectionS2C(newSelection).sendToPlayer(ctx.player as ServerPlayer)
        }

        is MsgSplicingTableGetDataC2S -> {
            SplicingTableMenu.getInstance(ctx.player)?.sendData(ctx.player as ServerPlayer)
        }

        is MsgSplicingTableNewStaffPatternC2S -> {
            val menu = SplicingTableMenu.getInstance(player) ?: return@queue
            val (newSelection, resolutionType) = menu.table.drawPattern(player, pattern, index, selection)
            MsgSplicingTableNewSelectionS2C(newSelection).sendToPlayer(player)
            MsgSplicingTableNewStaffPatternS2C(resolutionType, index).sendToPlayer(player)
        }
    }
}
