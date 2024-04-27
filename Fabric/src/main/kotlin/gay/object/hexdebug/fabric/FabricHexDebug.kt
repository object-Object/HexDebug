package gay.`object`.hexdebug.fabric

import gay.`object`.hexdebug.HexDebug
import net.fabricmc.api.ModInitializer

object FabricHexDebug : ModInitializer {
    override fun onInitialize() {
        HexDebug.init()
    }
}
