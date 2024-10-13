package gay.`object`.hexdebug.registry

import gay.`object`.hexdebug.blocks.focusholder.FocusHolderBlock
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlock
import gay.`object`.hexdebug.items.FocusHolderBlockItem
import net.minecraft.core.Registry
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.item.Item.Properties as ItemProperties
import net.minecraft.world.level.block.state.BlockBehaviour.Properties as BlockProperties

object HexDebugBlocks : HexDebugRegistrar<Block>(Registry.BLOCK_REGISTRY, { Registry.BLOCK }) {
    @JvmField
    val SPLICING_TABLE = blockItem("splicing_table", HexDebugItems.props) {
        SplicingTableBlock(slateish)
    }

    @JvmField
    val FOCUS_HOLDER = blockItem(
        "focus_holder",
        blockBuilder = { FocusHolderBlock(slateish) },
        itemBuilder = { FocusHolderBlockItem(it, HexDebugItems.props) },
    )

    private val slateish get() = BlockProperties.copy(Blocks.DEEPSLATE_TILES).strength(4f, 4f)

    private fun <T : Block> blockItem(name: String, props: ItemProperties, builder: () -> T) =
        blockItem(name, builder) { BlockItem(it, props) }

    private fun <B : Block, I : Item> blockItem(
        name: String,
        blockBuilder: () -> B,
        itemBuilder: (B) -> I,
    ): BlockItemEntry<B, I> {
        val blockEntry = register(name, blockBuilder)
        val itemEntry = HexDebugItems.register(name) { itemBuilder(blockEntry.value) }
        return BlockItemEntry(blockEntry, itemEntry)
    }

    class BlockItemEntry<B : Block, I : Item>(
        blockEntry: Entry<B>,
        val itemEntry: HexDebugRegistrar<Item>.Entry<I>,
    ) : Entry<B>(blockEntry) {
        val block by ::value
        val item by itemEntry::value
    }
}
