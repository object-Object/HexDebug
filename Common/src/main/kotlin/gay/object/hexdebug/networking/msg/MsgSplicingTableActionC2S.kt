package gay.`object`.hexdebug.networking.msg

import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.gui.SplicingTableMenu
import gay.`object`.hexdebug.networking.HexDebugMessageC2S
import gay.`object`.hexdebug.networking.HexDebugMessageCompanionC2S
import gay.`object`.hexdebug.splicing.Selection
import gay.`object`.hexdebug.splicing.SplicingTableAction
import gay.`object`.hexdebug.splicing.readSelection
import gay.`object`.hexdebug.splicing.writeSelection
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer

/** Requests the server to run a splicing table action. */
data class MsgSplicingTableActionC2S(val action: SplicingTableAction, val selection: Selection?) : HexDebugMessageC2S {
    companion object : HexDebugMessageCompanionC2S<MsgSplicingTableActionC2S> {
        override val type = MsgSplicingTableActionC2S::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgSplicingTableActionC2S(
            action = buf.readEnum(SplicingTableAction::class.java),
            selection = buf.readSelection(),
        )

        override fun MsgSplicingTableActionC2S.encode(buf: FriendlyByteBuf) {
            buf.writeEnum(action)
            buf.writeSelection(selection)
        }

        override fun MsgSplicingTableActionC2S.applyOnServer(ctx: PacketContext) = ctx.queue {
            val menu = SplicingTableMenu.getInstance(ctx.player) ?: return@queue
            val newSelection = menu.table.runAction(action, ctx.player as? ServerPlayer, selection)
            MsgSplicingTableNewSelectionS2C(newSelection).sendToPlayer(ctx.player as ServerPlayer)
        }
    }
}
