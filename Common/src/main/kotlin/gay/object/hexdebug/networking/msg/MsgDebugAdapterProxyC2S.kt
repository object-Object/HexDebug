package gay.`object`.hexdebug.networking.msg

import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.adapter.DebugAdapterManager
import gay.`object`.hexdebug.networking.HexDebugMessageC2S
import gay.`object`.hexdebug.networking.HexDebugMessageCompanionC2S
import net.minecraft.network.FriendlyByteBuf

data class MsgDebugAdapterProxyC2S(val content: String) : HexDebugMessageC2S {
    companion object : HexDebugMessageCompanionC2S<MsgDebugAdapterProxyC2S> {
        override val type = MsgDebugAdapterProxyC2S::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgDebugAdapterProxyC2S(
            buf.readUtf(),
        )

        override fun MsgDebugAdapterProxyC2S.encode(buf: FriendlyByteBuf) {
            buf.writeUtf(content)
        }

        override fun MsgDebugAdapterProxyC2S.applyOnServer(ctx: PacketContext) = ctx.queue {
            DebugAdapterManager[ctx.player]?.launcher?.handleMessage(content)
        }
    }
}
