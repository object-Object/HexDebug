package gay.`object`.hexdebug.forge

import gay.`object`.hexdebug.HexDebugClient
import gay.`object`.hexdebug.resources.splicing.SplicingTableIotasResourceReloadListener
import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import thedarkcolour.kotlinforforge.forge.LOADING_CONTEXT

object ForgeHexDebugClient {
    @Suppress("UNUSED_PARAMETER")
    fun init(event: FMLClientSetupEvent) {
        HexDebugClient.init()
        LOADING_CONTEXT.registerExtensionPoint(ConfigScreenFactory::class.java) {
            ConfigScreenFactory { _, parent -> HexDebugClient.getConfigScreen(parent) }
        }
    }

    fun registerClientReloadListeners(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(SplicingTableIotasResourceReloadListener)
    }
}
