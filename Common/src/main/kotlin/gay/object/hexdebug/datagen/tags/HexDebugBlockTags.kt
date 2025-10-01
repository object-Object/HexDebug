package gay.`object`.hexdebug.datagen.tags

import gay.`object`.hexdebug.registry.HexDebugBlocks
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.PackOutput
import net.minecraft.data.tags.TagsProvider
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.block.Block
import java.util.concurrent.CompletableFuture

class HexDebugBlockTags(output: PackOutput, provider: CompletableFuture<HolderLookup.Provider>)
    : TagsProvider<Block>(output, Registries.BLOCK, provider)
{
    override fun addTags(provider: HolderLookup.Provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
            HexDebugBlocks.SPLICING_TABLE.key,
            HexDebugBlocks.ENLIGHTENED_SPLICING_TABLE.key,
            HexDebugBlocks.FOCUS_HOLDER.key,
        )
    }
}
