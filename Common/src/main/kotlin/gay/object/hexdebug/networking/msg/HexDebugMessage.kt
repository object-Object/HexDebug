package gay.`object`.hexdebug.networking.msg

import dev.architectury.networking.NetworkChannel
import dev.architectury.networking.NetworkManager.PacketContext
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.networking.HexDebugNetworking
import gay.`object`.hexdebug.networking.handler.applyOnClient
import gay.`object`.hexdebug.networking.handler.applyOnServer
import net.fabricmc.api.EnvType
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import java.util.function.Supplier

sealed interface HexDebugMessage

sealed interface HexDebugMessageC2S : HexDebugMessage {
    fun sendToServer() {
        HexDebugNetworking.CHANNEL.sendToServer(this)
    }
}

sealed interface HexDebugMessageS2C : HexDebugMessage {
    fun sendToPlayer(player: ServerPlayer) {
        HexDebugNetworking.CHANNEL.sendToPlayer(player, this)
    }

    fun sendToPlayers(players: Iterable<ServerPlayer>) {
        HexDebugNetworking.CHANNEL.sendToPlayers(players, this)
    }
}

sealed interface HexDebugMessageCompanion<T : HexDebugMessage> {
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
