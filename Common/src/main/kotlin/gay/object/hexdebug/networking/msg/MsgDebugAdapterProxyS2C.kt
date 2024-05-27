package gay.`object`.hexdebug.networking.msg

import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.adapter.proxy.DebugProxyClient
import gay.`object`.hexdebug.networking.HexDebugMessageCompanionS2C
import gay.`object`.hexdebug.networking.HexDebugMessageS2C
import net.minecraft.network.FriendlyByteBuf

data class MsgDebugAdapterProxyS2C(val content: String) : HexDebugMessageS2C {
    companion object : HexDebugMessageCompanionS2C<MsgDebugAdapterProxyS2C> {
        override val type = MsgDebugAdapterProxyS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgDebugAdapterProxyS2C(
            buf.readUtf(),
        )

        override fun MsgDebugAdapterProxyS2C.encode(buf: FriendlyByteBuf) {
            buf.writeUtf(content)
        }

        override fun MsgDebugAdapterProxyS2C.applyOnClient(ctx: PacketContext) = ctx.queue {
            DebugProxyClient.instance?.consume(content)
        }
    }
}