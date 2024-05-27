package gay.`object`.hexdebug.networking.msg

import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.config.DebuggerDisplayMode
import gay.`object`.hexdebug.config.HexDebugConfig
import gay.`object`.hexdebug.networking.HexDebugMessageCompanionS2C
import gay.`object`.hexdebug.networking.HexDebugMessageS2C
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component

// we need a message for this because the client config isn't available on the server
data class MsgPrintDebuggerStatusS2C(
    val iota: String,
    val index: Int,
    val line: Int,
    val isConnected: Boolean,
) : HexDebugMessageS2C {
    companion object : HexDebugMessageCompanionS2C<MsgPrintDebuggerStatusS2C> {
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

        override fun MsgPrintDebuggerStatusS2C.applyOnClient(ctx: PacketContext) = ctx.queue {
            val config = HexDebugConfig.get().client
            val shouldPrint = when (config.debuggerDisplayMode) {
                DebuggerDisplayMode.DISABLED -> false
                DebuggerDisplayMode.NOT_CONNECTED -> !isConnected
                DebuggerDisplayMode.ENABLED -> true
            }

            if (shouldPrint) {
                ctx.player.displayClientMessage(
                    Component.translatable(
                        "text.hexdebug.debugger_stopped",
                        if (config.showDebugClientLineNumber) line else index,
                        iota,
                    ),
                    true,
                )
            }
        }
    }
}
