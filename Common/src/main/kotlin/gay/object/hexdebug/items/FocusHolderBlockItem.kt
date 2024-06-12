package gay.`object`.hexdebug.items

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.item.IotaHolderItem
import at.petrak.hexcasting.api.utils.asTranslatedComponent
import at.petrak.hexcasting.api.utils.getCompound
import at.petrak.hexcasting.api.utils.italic
import at.petrak.hexcasting.api.utils.putCompound
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.ContainerHelper
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block

class FocusHolderBlockItem(block: Block, properties: Properties) : BlockItem(block, properties), IotaHolderItem {
    override fun readIotaTag(stack: ItemStack): CompoundTag? {
        val (iotaStack, iotaHolder) = stack.getIotaStack()
        return iotaHolder?.readIotaTag(iotaStack)
    }

    override fun canWrite(stack: ItemStack, iota: Iota?): Boolean {
        val (iotaStack, iotaHolder) = stack.getIotaStack()
        return iotaHolder?.canWrite(iotaStack, iota) ?: false
    }

    override fun writeDatum(stack: ItemStack, iota: Iota?) {
        val (iotaStack, iotaHolder) = stack.getIotaStack()
        if (iotaHolder != null) {
            iotaHolder.writeDatum(iotaStack, iota)
            stack.putIotaStack(iotaStack)
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

    companion object {
        val ItemStack.styledHoverName: Component get() = Component.empty()
            .append(hoverName)
            .withStyle(rarity.color)
            .also { if (hasCustomHoverName()) it.italic }

        fun ItemStack.getIotaStack(): Pair<ItemStack, IotaHolderItem?> {
            val blockEntityTag = getCompound(BLOCK_ENTITY_TAG) ?: CompoundTag()

            val containerStacks = NonNullList.withSize(1, ItemStack.EMPTY)
            ContainerHelper.loadAllItems(blockEntityTag, containerStacks)

            val iotaStack = containerStacks.first()
            return Pair(iotaStack, iotaStack.item as? IotaHolderItem)
        }

        fun ItemStack.putIotaStack(iotaStack: ItemStack) {
            val blockEntityTag = getCompound(BLOCK_ENTITY_TAG) ?: CompoundTag()

            val containerStacks = NonNullList.of(ItemStack.EMPTY, iotaStack)
            ContainerHelper.saveAllItems(blockEntityTag, containerStacks)

            putCompound(BLOCK_ENTITY_TAG, blockEntityTag)
        }
    }
}
