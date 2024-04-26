package gay.`object`.hexdebug.networking

import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.adapter.proxy.DebugAdapterProxyClient
import net.minecraft.network.FriendlyByteBuf
import java.util.function.Supplier

data class MsgDebugAdapterProxyS2C(private val content: String) {
    constructor(buf: FriendlyByteBuf) : this(buf.readUtf())

    fun encode(buf: FriendlyByteBuf) {
        buf.writeUtf(content)
    }

    fun apply(supplier: Supplier<PacketContext>) = supplier.get().also { ctx ->
        ctx.queue {
            HexDebug.LOGGER.debug("Client received packet: {}", this)
            DebugAdapterProxyClient.instance?.consume(content)
        }
    }
}
