package gay.`object`.hexdebug.networking.msg

import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.blocks.splicing.ISplicingTable.Action
import gay.`object`.hexdebug.blocks.splicing.Selection
import gay.`object`.hexdebug.blocks.splicing.readSelection
import gay.`object`.hexdebug.blocks.splicing.writeSelection
import gay.`object`.hexdebug.gui.SplicingTableMenu
import gay.`object`.hexdebug.networking.HexDebugMessageC2S
import gay.`object`.hexdebug.networking.HexDebugMessageCompanionC2S
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer

/** Requests the server to run a splicing table action. */
data class MsgSplicingTableActionC2S(val action: Action, val selection: Selection?) : HexDebugMessageC2S {
    companion object : HexDebugMessageCompanionC2S<MsgSplicingTableActionC2S> {
        override val type = MsgSplicingTableActionC2S::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgSplicingTableActionC2S(
            action = buf.readEnum(Action::class.java),
            selection = buf.readSelection(),
        )

        override fun MsgSplicingTableActionC2S.encode(buf: FriendlyByteBuf) {
            buf.writeEnum(action)
            buf.writeSelection(selection)
        }

        override fun MsgSplicingTableActionC2S.applyOnServer(ctx: PacketContext) = ctx.queue {
            val menu = ctx.player.containerMenu as? SplicingTableMenu ?: return@queue
            val newSelection = menu.table.runAction(action, selection)
            MsgSplicingTableNewSelectionS2C(newSelection).sendToPlayer(ctx.player as ServerPlayer)
        }
    }
}
