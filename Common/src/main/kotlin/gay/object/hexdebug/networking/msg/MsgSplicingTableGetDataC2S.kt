package gay.`object`.hexdebug.networking.msg

import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.gui.SplicingTableMenu
import gay.`object`.hexdebug.networking.HexDebugMessageC2S
import gay.`object`.hexdebug.networking.HexDebugMessageCompanionC2S
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer

/** Asks the server to send a [MsgSplicingTableNewDataS2C] packet. */
class MsgSplicingTableGetDataC2S : HexDebugMessageC2S {
    companion object : HexDebugMessageCompanionC2S<MsgSplicingTableGetDataC2S> {
        override val type = MsgSplicingTableGetDataC2S::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgSplicingTableGetDataC2S()

        override fun MsgSplicingTableGetDataC2S.encode(buf: FriendlyByteBuf) {}

        override fun MsgSplicingTableGetDataC2S.applyOnServer(ctx: PacketContext) = ctx.queue {
            SplicingTableMenu.getInstance(ctx.player)?.sendData(ctx.player as ServerPlayer)
        }
    }
}
