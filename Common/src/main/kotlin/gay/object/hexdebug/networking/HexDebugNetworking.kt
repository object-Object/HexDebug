package gay.`object`.hexdebug.networking

import dev.architectury.networking.NetworkChannel
import gay.`object`.hexdebug.HexDebug
import net.minecraft.server.level.ServerPlayer

object HexDebugNetworking {
    private val CHANNEL = NetworkChannel.create(HexDebug.id("networking_channel"))

    fun init() {
        // FIXME: gross.
        CHANNEL.register(MsgDebugAdapterProxyC2S::class.java, MsgDebugAdapterProxyC2S::encode, ::MsgDebugAdapterProxyC2S, MsgDebugAdapterProxyC2S::apply)
        CHANNEL.register(MsgDebugAdapterProxyS2C::class.java, MsgDebugAdapterProxyS2C::encode, ::MsgDebugAdapterProxyS2C, MsgDebugAdapterProxyS2C::apply)
        CHANNEL.register(MsgDebuggerStateS2C::class.java, MsgDebuggerStateS2C::encode, ::MsgDebuggerStateS2C, MsgDebuggerStateS2C::apply)
        CHANNEL.register(MsgEvaluatorStateS2C::class.java, MsgEvaluatorStateS2C::encode, ::MsgEvaluatorStateS2C, MsgEvaluatorStateS2C::apply)
    }

    fun <T> sendToServer(message: T) = CHANNEL.sendToServer(message)

    fun <T> sendToPlayer(player: ServerPlayer, message: T) = CHANNEL.sendToPlayer(player, message)
}
