package gay.`object`.hexdebug.forge.datagen

import at.petrak.hexcasting.api.HexAPI
import at.petrak.paucal.api.forge.datagen.PaucalBlockStateAndModelProvider
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.blocks.focusholder.FocusHolderBlock
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlock
import gay.`object`.hexdebug.items.FocusHolderBlockItem
import gay.`object`.hexdebug.registry.HexDebugBlocks
import gay.`object`.hexdebug.utils.asItemPredicate
import net.minecraft.core.Direction
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraftforge.client.model.generators.BlockModelBuilder
import net.minecraftforge.client.model.generators.ConfiguredModel
import net.minecraftforge.client.model.generators.VariantBlockStateBuilder
import net.minecraftforge.common.data.ExistingFileHelper

@Suppress("SameParameterValue")
class HexDebugBlockModels(output: PackOutput, efh: ExistingFileHelper) : PaucalBlockStateAndModelProvider(output, HexDebug.MODID, efh) {
    override fun registerStatesAndModels() {
        horizontalBlockAndItem(HexDebugBlocks.SPLICING_TABLE, ::splicingTable)

        // https://github.com/FallingColors/HexMod/blob/871f9387a3e1ccf0231a3e90c31e5d8472d46fde/Forge/src/main/java/at/petrak/hexcasting/forge/datagen/xplat/HexBlockStatesAndModels.java#L245
        buildVariants(HexDebugBlocks.ENLIGHTENED_SPLICING_TABLE) { entry, builder ->
            builder.forAllStates { blockState ->
                val dir = blockState.getValue(SplicingTableBlock.FACING)
                val lit = blockState.getValue(SplicingTableBlock.IMBUED)

                val model = splicingTable(entry.id, lit)

                // most blocks point north in the inventory, but impetuses point east so the face isn't obscured by the item count
                // and this is basically an impetus, so let's do that
                if (lit && dir == Direction.EAST) {
                    simpleBlockItem(entry.block, model)
                }

                ConfiguredModel.builder()
                    .modelFile(model)
                    .rotationY((dir.toYRot().toInt() + 180) % 360)
                    .build()
            }
        }

        buildVariants(HexDebugBlocks.FOCUS_HOLDER) { entry, builder ->
            val itemModel = itemModels().getBuilder(entry.id.path)
            for (hasItem in arrayOf(false, true)) {
                val stateName = if (hasItem) "full" else "empty"
                val model = models()
                    .cubeAll("block/${entry.id.path}/$stateName", modLoc("block/focus_holder/$stateName"))
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
        entry: HexDebugBlocks.BlockItemEntry<*, *>,
        builder: (ResourceLocation) -> BlockModelBuilder,
    ) {
        val model = builder.invoke(entry.id)
        horizontalBlock(entry.block, model)
        simpleBlockItem(entry.block, model)
    }

    private fun <B : Block, I : Item> buildVariants(
        entry: HexDebugBlocks.BlockItemEntry<B, I>,
        builder: (HexDebugBlocks.BlockItemEntry<B, I>, VariantBlockStateBuilder) -> Unit,
    ) {
        builder.invoke(entry, getVariantBuilder(entry.block))
    }

    private fun splicingTable(id: ResourceLocation, lit: Boolean? = null): BlockModelBuilder {
        val litSuffix = when (lit) {
            true -> "/lit"
            false -> "/dim"
            null -> ""
        }
        return models()
            .cube(
                "block/${id.path}$litSuffix",
                HexAPI.modLoc("block/slate"), // down
                modLoc("block/${id.path}/top"), // up
                modLoc("block/${id.path}/front$litSuffix"), // north
                modLoc("block/${id.path}/back"), // south
                modLoc("block/${id.path}/right"), // east
                modLoc("block/${id.path}/left"), // west
            )
            .texture("particle", HexAPI.modLoc("block/slate"))
    }
}
