package gay.`object`.hexdebug.forge

import dev.architectury.platform.forge.EventBuses
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.forge.datagen.*
import net.minecraft.data.DataGenerator
import net.minecraft.data.DataProvider
import net.minecraft.data.loot.LootTableProvider
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
        event.apply {
            val efh = existingFileHelper
            addProvider(includeClient()) { HexDebugBlockModels(it, efh) }
            addProvider(includeClient()) { HexDebugItemModels(it, efh) }
            addProvider(includeServer()) { HexDebugRecipes(it) }
            addProvider(includeServer()) { HexDebugItemTags(it, efh) }
            addProvider(includeServer()) { HexDebugBlockTags(it, efh) }
            addProvider(includeServer()) {
                LootTableProvider(it, setOf(), listOf(
                    SubProviderEntry(::HexDebugBlockLootTables, LootContextParamSets.BLOCK),
                ))
            }
        }
    }
}

fun <T : DataProvider> GatherDataEvent.addProvider(run: Boolean, factory: (DataGenerator) -> T) =
    generator.addProvider(run, factory(generator))
