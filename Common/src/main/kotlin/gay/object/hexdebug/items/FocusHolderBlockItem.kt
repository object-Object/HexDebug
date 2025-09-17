package gay.`object`.hexdebug.items

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.item.IotaHolderItem
import at.petrak.hexcasting.api.utils.asTranslatedComponent
import at.petrak.hexcasting.api.utils.getList
import at.petrak.hexcasting.common.lib.HexItems
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.items.base.ItemPredicateProvider
import gay.`object`.hexdebug.items.base.ModelPredicateEntry
import gay.`object`.hexdebug.registry.HexDebugBlockEntities
import gay.`object`.hexdebug.registry.HexDebugBlocks
import gay.`object`.hexdebug.utils.asItemPredicate
import gay.`object`.hexdebug.utils.styledHoverName
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.world.ContainerHelper
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block

class FocusHolderBlockItem(block: Block, properties: Properties) :
    BlockItem(block, properties), IotaHolderItem, ItemPredicateProvider
{
    override fun readIotaTag(stack: ItemStack): CompoundTag? {
        val (iotaStack, iotaHolder) = stack.getIotaStack()
        return iotaHolder?.readIotaTag(iotaStack)
    }

    override fun writeable(stack: ItemStack): Boolean {
        val (_, iotaHolder) = stack.getIotaStack()
        return iotaHolder?.writeable(stack) ?: false
    }

    override fun canWrite(stack: ItemStack, iota: Iota?): Boolean {
        val (iotaStack, iotaHolder) = stack.getIotaStack()
        return iotaHolder?.canWrite(iotaStack, iota) ?: false
    }

    override fun writeDatum(stack: ItemStack, iota: Iota?) {
        val (iotaStack, iotaHolder) = stack.getIotaStack()
        if (iotaHolder != null) {
            iotaHolder.writeDatum(iotaStack, iota)
            stack.setIotaStack(iotaStack)
        }
    }

    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltipComponents: MutableList<Component>,
        isAdvanced: TooltipFlag
    ) {
        val (iotaStack, iotaHolder) = stack.getIotaStack()
        if (iotaHolder != null) {
            tooltipComponents += "hexdebug.tooltip.focus_holder.item".asTranslatedComponent(iotaStack.styledHoverName)
            IotaHolderItem.appendHoverText(iotaHolder, iotaStack, tooltipComponents, isAdvanced)
        }
    }

    override fun getModelPredicates() = listOf(
        ModelPredicateEntry(HAS_ITEM) { stack, _, _, _ ->
            stack.hasIotaStack.asItemPredicate
        },
    )

    companion object {
        val HAS_ITEM = HexDebug.id("has_item")

        fun withFocus(stack: ItemStack? = null): ItemStack {
            val result = stack?.copy() ?: ItemStack(HexDebugBlocks.FOCUS_HOLDER.item)
            return result.apply {
                setIotaStack(ItemStack(HexItems.FOCUS))
            }
        }

        val ItemStack.hasIotaStack get() = getBlockEntityData(this)
            ?.getList("Items", Tag.TAG_COMPOUND)
            ?.let { it.size > 0 }
            ?: false

        fun ItemStack.getIotaStack(): Pair<ItemStack, IotaHolderItem?> {
            val blockEntityTag = getBlockEntityData(this) ?: CompoundTag()

            val containerStacks = NonNullList.withSize(1, ItemStack.EMPTY)
            ContainerHelper.loadAllItems(blockEntityTag, containerStacks)

            val iotaStack = containerStacks.first()
            return Pair(iotaStack, iotaStack.item as? IotaHolderItem)
        }

        fun ItemStack.setIotaStack(iotaStack: ItemStack) {
            val tag = if (iotaStack.isEmpty) {
                CompoundTag()
            } else {
                val containerStacks = NonNullList.of(ItemStack.EMPTY, iotaStack)
                ContainerHelper.saveAllItems(getBlockEntityData(this) ?: CompoundTag(), containerStacks)
            }

            setBlockEntityData(this, HexDebugBlockEntities.FOCUS_HOLDER.value, tag)
        }
    }
}
