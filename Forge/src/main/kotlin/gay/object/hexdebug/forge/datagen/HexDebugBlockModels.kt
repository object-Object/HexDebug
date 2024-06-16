package gay.`object`.hexdebug.forge.datagen

import at.petrak.hexcasting.api.HexAPI
import at.petrak.paucal.api.forge.datagen.PaucalBlockStateAndModelProvider
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.blocks.focusholder.FocusHolderBlock
import gay.`object`.hexdebug.items.FocusHolderBlockItem
import gay.`object`.hexdebug.registry.HexDebugBlocks
import gay.`object`.hexdebug.registry.RegistrarEntry
import gay.`object`.hexdebug.utils.asItemPredicate
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import net.minecraftforge.client.model.generators.BlockModelBuilder
import net.minecraftforge.common.data.ExistingFileHelper

@Suppress("SameParameterValue")
class HexDebugBlockModels(output: PackOutput, efh: ExistingFileHelper) : PaucalBlockStateAndModelProvider(output, HexDebug.MODID, efh) {
    override fun registerStatesAndModels() {
        horizontalBlockAndItem(HexDebugBlocks.SPLICING_TABLE) { id ->
            models()
                .cube(
                    id.path,
                    HexAPI.modLoc("block/slate"), // down
                    modLoc("block/splicing_table/top"), // up
                    modLoc("block/splicing_table/front"), // north
                    modLoc("block/splicing_table/back"), // south
                    modLoc("block/splicing_table/right"), // east
                    modLoc("block/splicing_table/left"), // west
                )
                .texture("particle", HexAPI.modLoc("block/slate"))
        }


        getVariantBuilder(HexDebugBlocks.FOCUS_HOLDER.value).also { builder ->
            val id = HexDebugBlocks.FOCUS_HOLDER.id
            val itemModel = itemModels().getBuilder(id.path)
            for (hasItem in listOf(false, true)) {
                val stateName = if (hasItem) "full" else "empty"
                val model = models()
                    .cubeAll("block/${id.path}/$stateName", modLoc("block/focus_holder/$stateName"))
                    .texture("particle", HexAPI.modLoc("block/slate"))

                // item model override for this state
                itemModel.override()
                    .predicate(FocusHolderBlockItem.HAS_ITEM, hasItem.asItemPredicate)
                    .model(model)

                // blockstate override for this state
                builder.partialState()
                    .with(FocusHolderBlock.HAS_ITEM, hasItem)
                    .modelForState()
                    .modelFile(model)
                    .addModel()
            }
        }
    }

    private fun horizontalBlockAndItem(
        entry: RegistrarEntry<Block>,
        builder: (ResourceLocation) -> BlockModelBuilder,
    ) {
        val model = builder.invoke(entry.id)
        horizontalBlock(entry.value, model)
        simpleBlockItem(entry.value, model)
    }
}
