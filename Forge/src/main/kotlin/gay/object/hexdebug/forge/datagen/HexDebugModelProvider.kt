package gay.`object`.hexdebug.forge.datagen

import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.items.DebuggerState
import gay.`object`.hexdebug.items.ItemDebugger
import gay.`object`.hexdebug.items.itemOverride
import gay.`object`.hexdebug.registry.HexDebugItems
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.client.model.generators.ItemModelProvider
import net.minecraftforge.common.data.ExistingFileHelper

class HexDebugModelProvider(output: PackOutput, efh: ExistingFileHelper) : ItemModelProvider(output, HexDebug.MODID, efh) {
    override fun registerModels() {
        debugger(HexDebugItems.DEBUGGER.id)
    }

    private fun debugger(item: ResourceLocation) {
        val debugger = basicItem(item)
        val debuggerPath = debugger.location.path

        for (state in DebuggerState.entries) {
            if (state == DebuggerState.INACTIVE) continue

            val stateName = state.name.lowercase()
            val model = getBuilder("$debuggerPath/$stateName")
                .parent(debugger)
                .texture("layer1", "$debuggerPath/state/$stateName")

            debugger.override()
                .model(model)
                .predicate(ItemDebugger.DEBUGGER_STATE_ID, state.itemOverride)
                .end()
        }
    }
}
