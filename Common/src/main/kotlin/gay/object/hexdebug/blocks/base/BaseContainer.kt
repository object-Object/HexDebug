package gay.`object`.hexdebug.blocks.base

import net.minecraft.core.NonNullList
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/** Adapted from [https://fabricmc.net/wiki/tutorial:inventory](https://fabricmc.net/wiki/tutorial:inventory). */
interface BaseContainer : Container {
    val stacks: NonNullList<ItemStack>

    override fun getContainerSize() = stacks.size

    override fun isEmpty() = stacks.all { it.isEmpty }

    override fun getItem(slot: Int) = stacks[slot]

    override fun removeItem(slot: Int, amount: Int) = ContainerHelper.removeItem(stacks, slot, amount).also {
        if (!it.isEmpty) setChanged()
    }

    override fun removeItemNoUpdate(slot: Int) = ContainerHelper.takeItem(stacks, slot)

    override fun setItem(slot: Int, stack: ItemStack) {
        stacks[slot] = stack.apply {
            if (count > maxStackSize) count = maxStackSize
        }
    }

    override fun clearContent() {
        stacks.replaceAll { ItemStack.EMPTY }
    }

    override fun stillValid(player: Player) = true

    companion object {
        fun withSize(size: Int): NonNullList<ItemStack> = NonNullList.withSize(size, ItemStack.EMPTY)

        fun of(vararg items: ItemStack): NonNullList<ItemStack> = NonNullList.of(ItemStack.EMPTY, *items)
    }
}
