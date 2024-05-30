package gay.`object`.hexdebug.registry

import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

object HexDebugBlockEntities : HexDebugRegistrar<BlockEntityType<*>>(
    Registries.BLOCK_ENTITY_TYPE,
    { BuiltInRegistries.BLOCK_ENTITY_TYPE },
) {
    @JvmField
    val SPLICING_TABLE = register("splicing_table", ::SplicingTableBlockEntity) {
        arrayOf(HexDebugBlocks.SPLICING_TABLE.value)
    }

    private fun <T : BlockEntity> register(
        name: String,
        func: (BlockPos, BlockState) -> T,
        blocks: () -> Array<Block>,
    ) = register(name) { IXplatAbstractions.INSTANCE.createBlockEntityType(func, *blocks()) }
}
