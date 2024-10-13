package gay.`object`.hexdebug.forge.datagen

import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.registry.HexDebugBlocks
import net.minecraft.core.Registry
import net.minecraft.data.DataGenerator
import net.minecraft.data.tags.TagsProvider
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.block.Block
import net.minecraftforge.common.data.ExistingFileHelper

class HexDebugBlockTags(
    gen: DataGenerator,
    efh: ExistingFileHelper,
) : TagsProvider<Block>(gen, Registry.BLOCK, HexDebug.MODID, efh) {
    override fun addTags() {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
            HexDebugBlocks.SPLICING_TABLE.key, // TODO: this feels like it should be wood instead of slate
            HexDebugBlocks.FOCUS_HOLDER.key,
        )
    }
}
