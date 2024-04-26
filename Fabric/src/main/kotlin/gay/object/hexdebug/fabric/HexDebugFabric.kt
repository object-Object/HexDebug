package gay.`object`.hexdebug.fabric

import gay.`object`.hexdebug.HexDebug
import net.fabricmc.api.ModInitializer

object HexDebugFabric : ModInitializer {
    override fun onInitialize() {
        HexDebug.init()
        HexDebugConfigFabric.init()
    }
}
