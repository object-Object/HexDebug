package gay.`object`.hexdebug.networking

import dev.architectury.networking.NetworkChannel
import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.HexDebug
import net.minecraft.network.FriendlyByteBuf
import java.util.function.Supplier

interface HexDebugMessageCompanion<T : HexDebugMessage> {
    val type: Class<T>

    fun decode(buf: FriendlyByteBuf): T

    fun T.encode(buf: FriendlyByteBuf)

    fun register(channel: NetworkChannel)

    fun register(channel: NetworkChannel, apply: (T, Supplier<PacketContext>) -> Unit) {
        channel.register(type, { msg, buf -> msg.encode(buf) }, ::decode, apply)
    }
}

interface HexDebugMessageCompanionC2S<T : HexDebugMessageC2S> : HexDebugMessageCompanion<T> {
    fun T.applyOnServer(ctx: PacketContext)

    fun applyOnServer(msg: T, supplier: Supplier<PacketContext>) {
        HexDebug.LOGGER.debug("Client received packet: {}", this)
        msg.applyOnServer(supplier.get())
    }

    override fun register(channel: NetworkChannel) {
        register(channel, ::applyOnServer)
    }
}

interface HexDebugMessageCompanionS2C<T : HexDebugMessageS2C> : HexDebugMessageCompanion<T> {
    fun T.applyOnClient(ctx: PacketContext)

    fun applyOnClient(msg: T, supplier: Supplier<PacketContext>) {
        val ctx = supplier.get()
        HexDebug.LOGGER.debug("Server received packet from {}: {}", ctx.player.name.string, this)
        msg.applyOnClient(ctx)
    }

    override fun register(channel: NetworkChannel) {
        register(channel, ::applyOnClient)
    }
}
