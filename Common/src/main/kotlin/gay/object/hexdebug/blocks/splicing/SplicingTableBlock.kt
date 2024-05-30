package gay.`object`.hexdebug.blocks.splicing

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Containers
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

@Suppress("OVERRIDE_DEPRECATION")
class SplicingTableBlock(properties: Properties) : BaseEntityBlock(properties) {
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
}
