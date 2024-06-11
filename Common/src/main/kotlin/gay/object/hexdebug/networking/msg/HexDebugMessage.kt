package gay.`object`.hexdebug.networking

import dev.architectury.networking.NetworkChannel
import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.HexDebug
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import java.util.function.Supplier

interface HexDebugMessage

interface HexDebugMessageC2S : HexDebugMessage {
    fun applyOnServer(ctx: PacketContext)

    fun sendToServer() {
        HexDebugNetworking.CHANNEL.sendToServer(this)
    }
}

interface HexDebugMessageS2C : HexDebugMessage {
    @Environment(EnvType.CLIENT)
    fun applyOnClient(ctx: PacketContext)

    fun sendToPlayer(player: ServerPlayer) {
        HexDebugNetworking.CHANNEL.sendToPlayer(player, this)
    }

    fun sendToPlayers(players: Iterable<ServerPlayer>) {
        HexDebugNetworking.CHANNEL.sendToPlayers(players, this)
    }
}

interface HexDebugMessageCompanion<T : HexDebugMessage> {
    val type: Class<T>

    fun decode(buf: FriendlyByteBuf): T

    fun T.encode(buf: FriendlyByteBuf)

    fun apply(msg: T, supplier: Supplier<PacketContext>) {
        val ctx = supplier.get()
        when (ctx.env) {
            EnvType.SERVER, null -> {
                HexDebug.LOGGER.debug("Server received packet from {}: {}", ctx.player.name.string, this)
                when (msg) {
                    is HexDebugMessageC2S -> msg.applyOnServer(ctx)
                    else -> HexDebug.LOGGER.warn("Message not handled on server: {}", msg::class)
                }
            }
            EnvType.CLIENT -> {
                HexDebug.LOGGER.debug("Client received packet: {}", this)
                when (msg) {
                    is HexDebugMessageS2C -> msg.applyOnClient(ctx)
                    else -> HexDebug.LOGGER.warn("Message not handled on client: {}", msg::class)
                }
            }
        }
    }

    fun register(channel: NetworkChannel) {
        channel.register(type, { msg, buf -> msg.encode(buf) }, ::decode, ::apply)
    }
}
