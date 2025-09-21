package gay.`object`.hexdebug.fabric

import gay.`object`.hexdebug.HexDebug
import net.fabricmc.api.DedicatedServerModInitializer

object FabricHexDebugServer : DedicatedServerModInitializer {
    override fun onInitializeServer() {
        HexDebug.initServer()
    }
}
