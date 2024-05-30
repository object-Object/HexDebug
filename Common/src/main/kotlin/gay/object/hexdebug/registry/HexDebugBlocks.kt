package gay.`object`.hexdebug.registry

import gay.`object`.hexdebug.blocks.SplicingTableBlock
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.BlockItem
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.item.Item.Properties as ItemProperties
import net.minecraft.world.level.block.state.BlockBehaviour.Properties as BlockProperties

object HexDebugBlocks : HexDebugRegistrar<Block>(Registries.BLOCK, { BuiltInRegistries.BLOCK }) {
    @JvmField
    val SPLICING_TABLE = blockItem("splicing_table", HexDebugItems.props) {
        SplicingTableBlock(slateish.noPush())
    }

    private val slateish get() = BlockProperties.copy(Blocks.DEEPSLATE_TILES).strength(4f, 4f)

    private fun BlockProperties.noPush() = pushReaction(PushReaction.BLOCK)

    private fun <T : Block> blockItem(name: String, props: ItemProperties, builder: () -> T) =
        register(name, builder).also {
            HexDebugItems.register(name) { BlockItem(it.value, props) }
        }
}
