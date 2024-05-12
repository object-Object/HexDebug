@file:OptIn(ExperimentalStdlibApi::class)

package gay.`object`.hexdebug.forge.datagen

import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.items.ItemDebugger
import gay.`object`.hexdebug.items.ItemDebugger.DebugState
import gay.`object`.hexdebug.items.ItemDebugger.StepMode
import gay.`object`.hexdebug.registry.HexDebugItems
import gay.`object`.hexdebug.utils.itemPredicate
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.client.model.generators.ItemModelProvider
import net.minecraftforge.client.model.generators.ModelBuilder
import net.minecraftforge.common.data.ExistingFileHelper

class HexDebugModels(output: PackOutput, efh: ExistingFileHelper) : ItemModelProvider(output, HexDebug.MODID, efh) {
    override fun registerModels() {
        debugger(HexDebugItems.DEBUGGER.id)
    }

    private fun debugger(item: ResourceLocation) {
        val baseModel = basicItem(item)
        val basePath = baseModel.location.path

        for (debugState in DebugState.entries) {
            val debugStateName = debugState.name.lowercase()

            for (stepMode in StepMode.entries) {
                val stepModeName = stepMode.name.lowercase()

                for ((hasHex, hasHexName) in mapOf(0f to "empty", 1f to "full")) {
                    val model = getBuilder("$basePath/$debugStateName/$stepModeName/$hasHexName")
                        .parent(baseModel)
                        .layers(
                            start = 1,
                            "$basePath/step_mode/$debugStateName/$stepModeName",
                            "$basePath/has_hex/$hasHexName".takeIf { hasHex > 0 },
                            "$basePath/debug_state/$debugStateName".takeIf { debugState == DebugState.DEBUGGING },
                        )

                    baseModel.override()
                        .model(model)
                        .predicate(ItemDebugger.DEBUG_STATE_PREDICATE, debugState.itemPredicate)
                        .predicate(ItemDebugger.STEP_MODE_PREDICATE, stepMode.itemPredicate)
                        .predicate(ItemDebugger.HAS_HEX_PREDICATE, hasHex)
                }
            }
        }

        // should be last so it overrides everything else
        baseModel.override()
            .predicate(ItemDebugger.HIDE_ICONS_PREDICATE, 1f)
            .model(baseModel)
    }
}

fun <T : ModelBuilder<T>> T.layers(start: Int, vararg layers: String?): T {
    var index = start
    for (layer in layers) {
        if (layer != null) {
            texture("layer$index", layer)
            index += 1
        }
    }
    return this
}
