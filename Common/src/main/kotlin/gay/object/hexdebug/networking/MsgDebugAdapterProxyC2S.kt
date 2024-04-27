package gay.`object`.hexdebug.networking

import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.adapter.DebugAdapterManager
import net.minecraft.network.FriendlyByteBuf
import java.util.function.Supplier

data class MsgDebugAdapterProxyC2S(private val content: String) {
    constructor(buf: FriendlyByteBuf) : this(buf.readUtf())

    fun encode(buf: FriendlyByteBuf) {
        buf.writeUtf(content)
    }

    fun apply(supplier: Supplier<PacketContext>) = supplier.get().also { ctx ->
        ctx.queue {
            HexDebug.LOGGER.debug("Server received packet from {}: {}", ctx.player.name.string, this)
            DebugAdapterManager[ctx.player]?.launcher?.handleMessage(content)
        }
    }
}
