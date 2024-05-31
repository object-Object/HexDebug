package gay.`object`.hexdebug.forge.datagen

import at.petrak.hexcasting.api.HexAPI
import at.petrak.paucal.api.forge.datagen.PaucalBlockStateAndModelProvider
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.registry.HexDebugBlocks
import gay.`object`.hexdebug.registry.RegistrarEntry
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import net.minecraftforge.client.model.generators.BlockModelBuilder
import net.minecraftforge.client.model.generators.BlockModelProvider
import net.minecraftforge.common.data.ExistingFileHelper

class HexDebugBlockModels(output: PackOutput, efh: ExistingFileHelper) : PaucalBlockStateAndModelProvider(output, HexDebug.MODID, efh) {
    override fun registerStatesAndModels() {
        horizontalBlockAndItem(HexDebugBlocks.SPLICING_TABLE) { id ->
            cube(
                id.path,
                HexAPI.modLoc("block/slate"), // down
                modLoc("block/splicing_table/top"), // up
                modLoc("block/splicing_table/front"), // north
                modLoc("block/splicing_table/back"), // south
                modLoc("block/splicing_table/right"), // east
                modLoc("block/splicing_table/left"), // west
            ).texture("particle", HexAPI.modLoc("block/slate"))
        }
    }

    private fun horizontalBlockAndItem(
        entry: RegistrarEntry<Block>,
        builder: BlockModelProvider.(ResourceLocation) -> BlockModelBuilder,
    ) {
        val model = builder.invoke(models(), entry.id)
        horizontalBlock(entry.value, model)
        simpleBlockItem(entry.value, model)
    }
}
