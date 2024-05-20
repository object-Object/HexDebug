@file:OptIn(ExperimentalStdlibApi::class)

package gay.`object`.hexdebug.forge.datagen

import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.items.ItemDebugger
import gay.`object`.hexdebug.items.ItemDebugger.DebugState
import gay.`object`.hexdebug.items.ItemDebugger.StepMode
import gay.`object`.hexdebug.items.ItemEvaluator
import gay.`object`.hexdebug.items.ItemEvaluator.EvalState
import gay.`object`.hexdebug.registry.HexDebugItems
import gay.`object`.hexdebug.utils.itemPredicate
import net.minecraft.data.DataGenerator
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.client.model.generators.ItemModelProvider
import net.minecraftforge.client.model.generators.ModelBuilder
import net.minecraftforge.client.model.generators.ModelFile
import net.minecraftforge.common.data.ExistingFileHelper

class HexDebugModels(gen: DataGenerator, efh: ExistingFileHelper) : ItemModelProvider(gen, HexDebug.MODID, efh) {
    override fun registerModels() {
        debugger(HexDebugItems.DEBUGGER.id)

        evaluator(HexDebugItems.EVALUATOR.id)
    }

    private fun debugger(item: ResourceLocation) {
        val baseModel = basicItem(item)
            .parent(ModelFile.UncheckedModelFile("item/handheld_rod"))

        val basePath = baseModel.location.path

        for (debugState in DebugState.values()) {
            val debugStateName = debugState.name.lowercase()

            for (stepMode in StepMode.values()) {
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
                        .predicate(ItemDebugger.DEBUG_STATE_PREDICATE, debugState.itemPredicate(DebugState.values()))
                        .predicate(ItemDebugger.STEP_MODE_PREDICATE, stepMode.itemPredicate(StepMode.values()))
                        .predicate(ItemDebugger.HAS_HEX_PREDICATE, hasHex)
                }
            }
        }

        // should be last so it overrides everything else
        baseModel.override()
            .predicate(ItemDebugger.HIDE_ICONS_PREDICATE, 1f)
            .model(baseModel)
    }

    private fun evaluator(item: ResourceLocation) {
        val baseModel = basicItem(item)
            .parent(ModelFile.UncheckedModelFile("item/handheld_rod"))

        val basePath = baseModel.location.path

        for (evalState in EvalState.entries) {
            val evalStateName = evalState.name.lowercase()

            val model = getBuilder("$basePath/$evalStateName")
                .parent(baseModel)
                .layers(
                    start = 1,
                    "$basePath/eval_state/$evalStateName".takeIf { evalState == EvalState.MODIFIED },
                )

            baseModel.override()
                .model(model)
                .predicate(ItemEvaluator.EVAL_STATE_PREDICATE, evalState.itemPredicate)
        }
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
