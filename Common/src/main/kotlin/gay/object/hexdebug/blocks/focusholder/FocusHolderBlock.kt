package gay.`object`.hexdebug.blocks.focusholder

import gay.`object`.hexdebug.utils.isNotEmpty
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.BlockHitResult

@Suppress("OVERRIDE_DEPRECATION")
class FocusHolderBlock(properties: Properties) : BaseEntityBlock(properties) {
    init {
        registerDefaultState(
            getStateDefinition().any()
                .setValue(HAS_ITEM, false)
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(HAS_ITEM)
    }

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
        // don't insert/remove items if sneaking or using offhand
        if (player.isShiftKeyDown || hand == InteractionHand.OFF_HAND) {
            return InteractionResult.PASS
        }

        val blockEntity = getBlockEntity(level, pos) ?: return InteractionResult.PASS
        val heldItem = player.getItemInHand(hand)
        val storedItem = blockEntity.iotaStack

        fun swapItem(): InteractionResult {
            if (!level.isClientSide) {
                player.setItemInHand(hand, storedItem)
                blockEntity.iotaStack = heldItem
                // TODO: there's probably a way to not send two events here
                blockEntity.sync()
            }
            return InteractionResult.sidedSuccess(level.isClientSide)
        }

        return if (FocusHolderBlockEntity.isValidItem(heldItem)) {
            // main hand has valid item, swap with stored
            swapItem()
        } else if (heldItem.isNotEmpty) {
            // main hand has invalid item, use it
            InteractionResult.PASS
        } else if (storedItem.isNotEmpty) {
            // block has stored item, take it
            swapItem()
        } else {
            // block and hand are empty, try other hand
            InteractionResult.PASS
        }
    }

    override fun playerWillDestroy(level: Level, pos: BlockPos, state: BlockState, player: Player) {
        // if broken in creative, drop with contents
        getBlockEntity(level, pos)?.let { blockEntity ->
            if (!level.isClientSide && !blockEntity.isEmpty && player.isCreative) {
                val stack = ItemStack(this)
                blockEntity.saveToItem(stack)
                ItemEntity(level, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, stack).run {
                    setDefaultPickUpDelay()
                    level.addFreshEntity(this)
                }
            }
        }
        super.playerWillDestroy(level, pos, state, player)
    }

    override fun getCloneItemStack(level: BlockGetter, pos: BlockPos, state: BlockState): ItemStack {
        val stack = super.getCloneItemStack(level, pos, state)
        getBlockEntity(level, pos)?.saveToItem(stack)
        return stack
    }

    override fun getDrops(state: BlockState, params: LootParams.Builder): MutableList<ItemStack> {
        val lootTableDrops = super.getDrops(state, params)

        val blockEntity = params.getBlockEntity<FocusHolderBlockEntity>()
        if (blockEntity == null || blockEntity.isEmpty) {
            // drop without NBT (ie. stackable with newly crafted items) if not holding an item
            return lootTableDrops
        }

        if (lootTableDrops.isEmpty()) {
            // block was destroyed; just drop the contained item
            return blockEntity.stacks
        }

        val stack = ItemStack(this)
        blockEntity.saveToItem(stack)
        return mutableListOf(stack)
    }

    override fun hasAnalogOutputSignal(state: BlockState) = true

    override fun getAnalogOutputSignal(state: BlockState, level: Level, pos: BlockPos) =
        getBlockEntity(level, pos)?.analogOutputSignal ?: 0

    companion object {
        val HAS_ITEM: BooleanProperty = BooleanProperty.create("has_item")

        fun getBlockEntity(level: BlockGetter, pos: BlockPos) = level.getBlockEntity(pos) as? FocusHolderBlockEntity
    }
}

inline fun <reified T : BlockEntity> LootParams.Builder.getBlockEntity(): T? {
    return getOptionalParameter(LootContextParams.BLOCK_ENTITY) as? T
}
