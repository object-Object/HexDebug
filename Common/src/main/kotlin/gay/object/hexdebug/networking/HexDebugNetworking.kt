package gay.`object`.hexdebug.networking

import dev.architectury.networking.NetworkChannel
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.networking.msg.HexDebugMessageCompanion

object HexDebugNetworking {
    val CHANNEL: NetworkChannel = NetworkChannel.create(HexDebug.id("networking_channel"))

    fun init() {
        for (subclass in HexDebugMessageCompanion::class.sealedSubclasses) {
            subclass.objectInstance?.register(CHANNEL)
        }
    }
}
