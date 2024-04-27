package gay.`object`.hexdebug.forge

import gay.`object`.hexdebug.HexDebugClient
import gay.`object`.hexdebug.config.HexDebugConfig
import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import thedarkcolour.kotlinforforge.forge.LOADING_CONTEXT

object ForgeHexDebugClient {
    fun init(event: FMLClientSetupEvent) {
        HexDebugClient.init()
        LOADING_CONTEXT.registerExtensionPoint(ConfigScreenFactory::class.java) {
            ConfigScreenFactory { _, parent -> HexDebugConfig.getConfigScreen(parent) }
        }
    }
}
