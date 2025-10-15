package gay.`object`.hexdebug.datagen.tags

import at.petrak.hexcasting.api.mod.HexTags
import gay.`object`.hexdebug.api.HexDebugTags
import gay.`object`.hexdebug.registry.HexDebugBlocks
import gay.`object`.hexdebug.registry.HexDebugItems
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.PackOutput
import net.minecraft.data.tags.TagsProvider
import net.minecraft.world.item.Item
import java.util.concurrent.CompletableFuture

class HexDebugItemTags(output: PackOutput, provider: CompletableFuture<HolderLookup.Provider>)
    : TagsProvider<Item>(output, Registries.ITEM, provider)
{
    override fun addTags(provider: HolderLookup.Provider) {
        tag(HexDebugTags.Items.FOCUS_HOLDER_BLACKLIST).add(
            HexDebugBlocks.FOCUS_HOLDER.itemKey,
        )

        tag(HexTags.Items.STAVES).add(
            HexDebugItems.EVALUATOR.key,
            HexDebugItems.QUENCHED_EVALUATOR.key,
        )
    }
}
