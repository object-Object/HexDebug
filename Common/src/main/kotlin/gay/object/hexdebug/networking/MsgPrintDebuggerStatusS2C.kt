package gay.`object`.hexdebug.networking

import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.config.DebuggerDisplayMode
import gay.`object`.hexdebug.config.HexDebugConfig
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import java.util.function.Supplier

// we need a message for this because the client config isn't available on the server
data class MsgPrintDebuggerStatusS2C(
    val iota: String,
    val index: Int,
    val line: Int,
    val isConnected: Boolean,
) {
    constructor(buf: FriendlyByteBuf) : this(
        buf.readUtf(),
        buf.readInt(),
        buf.readInt(),
        buf.readBoolean(),
    )

    fun encode(buf: FriendlyByteBuf) {
        buf.writeUtf(iota)
        buf.writeInt(index)
        buf.writeInt(line)
        buf.writeBoolean(isConnected)
    }

    fun apply(supplier: Supplier<PacketContext>) = supplier.get().also { ctx ->
        ctx.queue {
            HexDebug.LOGGER.debug("Client received packet: {}", this)

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