package gay.`object`.hexdebug.blocks.splicing

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Containers
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.phys.BlockHitResult

@Suppress("OVERRIDE_DEPRECATION")
class SplicingTableBlock(properties: Properties) : BaseEntityBlock(properties) {
    init {
        registerDefaultState(
            getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext) =
        defaultBlockState().setValue(FACING, ctx.horizontalDirection.opposite)

    override fun mirror(state: BlockState, mirror: Mirror) =
        state.rotate(mirror.getRotation(state.getValue(FACING)))

    override fun rotate(state: BlockState, rotation: Rotation) =
        state.setValue(FACING, rotation.rotate(state.getValue(FACING)))

    override fun newBlockEntity(pos: BlockPos, state: BlockState) = SplicingTableBlockEntity(pos, state)

    override fun getRenderShape(state: BlockState) = RenderShape.MODEL

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            state.getMenuProvider(level, pos)?.let {
                (player as ServerPlayer).openMenu(it)
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        movedByPiston: Boolean
    ) {
        if (state.block != newState.block) {
            getBlockEntity(level, pos)?.let {
                Containers.dropContents(level, pos, it)
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston)
    }

    override fun hasAnalogOutputSignal(state: BlockState) = true

    override fun getAnalogOutputSignal(state: BlockState, level: Level, pos: BlockPos) =
        getBlockEntity(level, pos)?.analogOutputSignal ?: 0

    private fun getBlockEntity(level: Level, pos: BlockPos) = level.getBlockEntity(pos) as? SplicingTableBlockEntity

    override fun getPistonPushReaction(state: BlockState) = PushReaction.BLOCK

    companion object {
        val FACING = BlockStateProperties.HORIZONTAL_FACING
    }
}
