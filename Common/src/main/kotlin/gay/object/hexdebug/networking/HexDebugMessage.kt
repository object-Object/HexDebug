package gay.`object`.hexdebug.networking

import net.minecraft.server.level.ServerPlayer

interface HexDebugMessage

interface HexDebugMessageC2S : HexDebugMessage {
    fun sendToServer() = HexDebugNetworking.sendToServer(this)
}

interface HexDebugMessageS2C : HexDebugMessage {
    fun sendToPlayer(player: ServerPlayer) = HexDebugNetworking.sendToPlayer(player, this)
}
