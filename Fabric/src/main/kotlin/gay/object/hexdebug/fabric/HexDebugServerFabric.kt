package gay.`object`.hexdebug.fabric

import gay.`object`.hexdebug.HexDebugServer
import net.fabricmc.api.DedicatedServerModInitializer

object HexDebugServerFabric : DedicatedServerModInitializer {
    override fun onInitializeServer() {
        HexDebugServer.init()
    }
}
