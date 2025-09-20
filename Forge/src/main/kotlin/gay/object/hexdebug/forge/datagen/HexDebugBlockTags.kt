package gay.`object`.hexdebug.forge.datagen

import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.registry.HexDebugBlocks
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.PackOutput
import net.minecraft.data.tags.TagsProvider
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.block.Block
import net.minecraftforge.common.data.ExistingFileHelper
import java.util.concurrent.CompletableFuture

class HexDebugBlockTags(
    output: PackOutput,
    registries: CompletableFuture<HolderLookup.Provider>,
    efh: ExistingFileHelper,
) : TagsProvider<Block>(output, Registries.BLOCK, registries, HexDebug.MODID, efh) {
    override fun addTags(provider: HolderLookup.Provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
            HexDebugBlocks.SPLICING_TABLE.key,
            HexDebugBlocks.ENLIGHTENED_SPLICING_TABLE.key,
            HexDebugBlocks.FOCUS_HOLDER.key,
        )
    }
}
