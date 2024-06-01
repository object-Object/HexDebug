package gay.`object`.hexdebug.networking

import dev.architectury.networking.NetworkChannel
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.networking.msg.*
import net.minecraft.server.level.ServerPlayer

object HexDebugNetworking {
    private val CHANNEL = NetworkChannel.create(HexDebug.id("networking_channel"))

    fun init() {
        val messages = listOf(
            MsgDebugAdapterProxyC2S,
            MsgDebugAdapterProxyS2C,
            MsgDebuggerStateS2C,
            MsgEvaluatorStateS2C,
            MsgPrintDebuggerStatusS2C,
            MsgSplicingTableActionC2S,
            MsgSplicingTableNewSelectionS2C,
            MsgSplicingTableNewDataS2C,
        )
        for (message in messages) {
            message.register(CHANNEL)
        }
    }

    fun <T> sendToServer(message: T) = CHANNEL.sendToServer(message)

    fun <T> sendToPlayer(player: ServerPlayer, message: T) = CHANNEL.sendToPlayer(player, message)
}
