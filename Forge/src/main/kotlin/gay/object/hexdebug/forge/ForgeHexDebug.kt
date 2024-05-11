package gay.`object`.hexdebug.forge

import dev.architectury.platform.forge.EventBuses
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.forge.datagen.HexDebugModels
import gay.`object`.hexdebug.forge.datagen.HexDebugRecipes
import net.minecraft.data.DataProvider.Factory
import net.minecraftforge.data.event.GatherDataEvent
import net.minecraftforge.fml.common.Mod
import thedarkcolour.kotlinforforge.forge.MOD_BUS

/**
 * This is your loading entrypoint on forge, in case you need to initialize
 * something platform-specific.
 */
@Mod(HexDebug.MODID)
class HexDebugForge {
    init {
        MOD_BUS.apply {
            EventBuses.registerModEventBus(HexDebug.MODID, this)
            addListener(ForgeHexDebugClient::init)
            addListener(::gatherData)
        }
        HexDebug.init()
    }

    private fun gatherData(event: GatherDataEvent) {
        val efh = event.existingFileHelper
        event.generator.apply {
            addProvider(event.includeClient(), Factory { HexDebugModels(it, efh) })
            addProvider(event.includeServer(), Factory { HexDebugRecipes(it) })
        }
    }
}
