@file:OptIn(ExperimentalStdlibApi::class)

package gay.`object`.hexdebug.forge.datagen

import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.items.DebuggerItem
import gay.`object`.hexdebug.items.DebuggerItem.DebugState
import gay.`object`.hexdebug.items.DebuggerItem.StepMode
import gay.`object`.hexdebug.items.EvaluatorItem
import gay.`object`.hexdebug.items.EvaluatorItem.EvalState
import gay.`object`.hexdebug.registry.HexDebugItems
import gay.`object`.hexdebug.utils.asItemPredicate
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.client.model.generators.ItemModelProvider
import net.minecraftforge.client.model.generators.ModelBuilder
import net.minecraftforge.common.data.ExistingFileHelper

class HexDebugItemModels(output: PackOutput, efh: ExistingFileHelper) : ItemModelProvider(output, HexDebug.MODID, efh) {
    override fun registerModels() {
        debugger(HexDebugItems.DEBUGGER.id)
        debugger(HexDebugItems.QUENCHED_DEBUGGER.id)

        evaluator(HexDebugItems.EVALUATOR.id)
        evaluator(HexDebugItems.QUENCHED_EVALUATOR.id)
    }

    private fun debugger(item: ResourceLocation) {
        val baseModel = basicItem(item)
            .parent(getExistingFile(mcLoc("item/handheld_rod")))

        for (debugState in DebugState.entries) {
            val debugStateName = debugState.name.lowercase()

            for (stepMode in StepMode.entries) {
                val stepModeName = stepMode.name.lowercase()

                for ((hasHex, hasHexName) in mapOf(0f to "empty", 1f to "full")) {
                    val model = getBuilder("${baseModel.location.path}/$debugStateName/$stepModeName/$hasHexName")
                        .parent(baseModel)
                        .layers(
                            start = 1,
                            "item/debugger/step_mode/$debugStateName/$stepModeName",
                            "item/debugger/has_hex/$hasHexName".takeIf { hasHex > 0 },
                            "item/debugger/debug_state/$debugStateName".takeIf { debugState == DebugState.DEBUGGING },
                        )

                    baseModel.override()
                        .model(model)
                        .predicate(DebuggerItem.DEBUG_STATE_PREDICATE, debugState.asItemPredicate)
                        .predicate(DebuggerItem.STEP_MODE_PREDICATE, stepMode.asItemPredicate)
                        .predicate(DebuggerItem.HAS_HEX_PREDICATE, hasHex)
                }
            }
        }

        // should be last so it overrides everything else
        baseModel.override()
            .predicate(DebuggerItem.HIDE_ICONS_PREDICATE, 1f)
            .model(baseModel)
    }

    private fun evaluator(item: ResourceLocation) {
        val baseModel = basicItem(item)
            .parent(getExistingFile(mcLoc("item/handheld_rod")))

        for (evalState in EvalState.entries) {
            val evalStateName = evalState.name.lowercase()

            val model = getBuilder("${baseModel.location.path}/$evalStateName")
                .parent(baseModel)
                .layers(
                    start = 1,
                    "item/evaluator/eval_state/$evalStateName".takeIf { evalState == EvalState.MODIFIED },
                )

            baseModel.override()
                .model(model)
                .predicate(EvaluatorItem.EVAL_STATE_PREDICATE, evalState.asItemPredicate)
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
