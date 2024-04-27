package gay.`object`.hexdebug.forge.datagen

import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.items.ItemDebugger
import gay.`object`.hexdebug.registry.HexDebugItems
import gay.`object`.hexdebug.utils.itemPredicate
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.client.model.generators.ItemModelProvider
import net.minecraftforge.common.data.ExistingFileHelper

class HexDebugModelProvider(output: PackOutput, efh: ExistingFileHelper) : ItemModelProvider(output, HexDebug.MODID, efh) {
    override fun registerModels() {
        debugger(HexDebugItems.DEBUGGER.id)
    }

    private fun debugger(item: ResourceLocation) {
        val baseModel = basicItem(item)
        val basePath = baseModel.location.path

        for (stepMode in ItemDebugger.StepMode.entries) {
            val stepModeName = stepMode.name.lowercase()

            for (state in ItemDebugger.State.entries) {
                val stateName = state.name.lowercase()

                val model = getBuilder("$basePath/$stepModeName/$stateName")
                    .parent(baseModel)
                    .texture("layer1", "$basePath/step_mode/$stepModeName")

                val override = baseModel.override()
                    .model(model)
                    .predicate(ItemDebugger.STEP_MODE_PREDICATE, stepMode.itemPredicate)

                if (state != ItemDebugger.State.INACTIVE) {
                    model.texture("layer2", "$basePath/state/$stateName")
                    override.predicate(ItemDebugger.STATE_PREDICATE, state.itemPredicate)
                }
            }
        }
    }
}
