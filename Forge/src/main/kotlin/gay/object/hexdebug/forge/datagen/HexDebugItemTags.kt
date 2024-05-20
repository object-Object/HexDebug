package gay.`object`.hexdebug.forge.datagen

import at.petrak.hexcasting.api.mod.HexTags
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.registry.HexDebugItems
import net.minecraft.core.Registry
import net.minecraft.data.DataGenerator
import net.minecraft.data.tags.TagsProvider
import net.minecraft.world.item.Item
import net.minecraftforge.common.data.ExistingFileHelper

class HexDebugItemTags(
    gen: DataGenerator,
    efh: ExistingFileHelper,
) : TagsProvider<Item>(gen, Registry.ITEM, HexDebug.MODID, efh) {
    override fun addTags() {
        tag(HexTags.Items.STAVES)
            .add(HexDebugItems.EVALUATOR.key)
    }
}
