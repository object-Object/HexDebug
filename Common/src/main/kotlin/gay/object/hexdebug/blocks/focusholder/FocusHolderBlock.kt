package gay.`object`.hexdebug.blocks.focusholder

import gay.`object`.hexdebug.registry.HexDebugBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.BlockHitResult

@Suppress("OVERRIDE_DEPRECATION")
class FocusHolderBlock(properties: Properties) : BaseEntityBlock(properties) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState) = FocusHolderBlockEntity(pos, state)

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

    override fun playerWillDestroy(level: Level, pos: BlockPos, state: BlockState, player: Player) {
        // if broken in creative, drop with contents (like a shulker box)
        (level.getBlockEntity(pos) as? FocusHolderBlockEntity)?.let { blockEntity ->
            if (!level.isClientSide && !blockEntity.isEmpty && player.isCreative) {
                val stack = ItemStack(this)
                blockEntity.saveToItem(stack)
                ItemEntity(level, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, stack).also {
                    it.setDefaultPickUpDelay()
                    level.addFreshEntity(it)
                }
            }
        }
        super.playerWillDestroy(level, pos, state, player)
    }

    override fun getCloneItemStack(blockGetter: BlockGetter, pos: BlockPos, state: BlockState): ItemStack {
        val stack = super.getCloneItemStack(blockGetter, pos, state)
        blockGetter.getBlockEntity(pos, HexDebugBlockEntities.FOCUS_HOLDER.value).ifPresent {
            it.saveToItem(stack)
        }
        return stack
    }

    override fun getDrops(state: BlockState, params: LootParams.Builder): MutableList<ItemStack> {
        val blockEntity = params.getBlockEntity<FocusHolderBlockEntity>()
        val newParams = if (blockEntity != null && !blockEntity.isEmpty) {
            params.withDynamicDrop(CONTENTS) {
                it.accept(blockEntity.focusStack)
            }
        } else {
            params
        }
        return super.getDrops(state, newParams)
    }

    companion object {
        val CONTENTS = ResourceLocation("contents")
    }
}

inline fun <reified T : BlockEntity> LootParams.Builder.getBlockEntity(): T? {
    return getOptionalParameter(LootContextParams.BLOCK_ENTITY) as? T
}
