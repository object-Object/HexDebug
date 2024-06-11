package gay.`object`.hexdebug.networking.msg

import net.minecraft.network.FriendlyByteBuf

data class MsgDebugAdapterProxy(val content: String) : HexDebugMessageC2S, HexDebugMessageS2C {
    companion object : HexDebugMessageCompanion<MsgDebugAdapterProxy> {
        override val type = MsgDebugAdapterProxy::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgDebugAdapterProxy(
            buf.readUtf(),
        )

        override fun MsgDebugAdapterProxy.encode(buf: FriendlyByteBuf) {
            buf.writeUtf(content)
        }
    }
}
