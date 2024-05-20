package gay.`object`.hexdebug.forge.datagen

import at.petrak.hexcasting.api.mod.HexTags
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.registry.HexDebugItems
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.data.PackOutput
import net.minecraft.data.tags.TagsProvider
import net.minecraft.world.item.Item
import net.minecraftforge.common.data.ExistingFileHelper
import java.util.concurrent.CompletableFuture

class HexDebugItemTags(
    output: PackOutput,
    registries: CompletableFuture<HolderLookup.Provider>,
    efh: ExistingFileHelper,
) : TagsProvider<Item>(output, Registries.ITEM, registries, HexDebug.MODID, efh) {
    override fun addTags(provider: HolderLookup.Provider) {
        tag(HexTags.Items.STAVES)
            .add(HexDebugItems.EVALUATOR.key)
    }
}
