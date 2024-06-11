package gay.`object`.hexdebug.networking.msg

import net.minecraft.network.FriendlyByteBuf

// we need a message for this because the client config isn't available on the server
data class MsgPrintDebuggerStatusS2C(
    val iota: String,
    val index: Int,
    val line: Int,
    val isConnected: Boolean,
) : HexDebugMessageS2C {
    companion object : HexDebugMessageCompanion<MsgPrintDebuggerStatusS2C> {
        override val type = MsgPrintDebuggerStatusS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgPrintDebuggerStatusS2C(
            buf.readUtf(),
            buf.readInt(),
            buf.readInt(),
            buf.readBoolean(),
        )

        override fun MsgPrintDebuggerStatusS2C.encode(buf: FriendlyByteBuf) {
            buf.writeUtf(iota)
            buf.writeInt(index)
            buf.writeInt(line)
            buf.writeBoolean(isConnected)
        }
    }
}
