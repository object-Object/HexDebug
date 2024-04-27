package gay.`object`.hexdebug.fabric

import gay.`object`.hexdebug.HexDebugClient
import net.fabricmc.api.ClientModInitializer

object FabricHexDebugClient : ClientModInitializer {
    override fun onInitializeClient() {
        HexDebugClient.init()
    }
}
