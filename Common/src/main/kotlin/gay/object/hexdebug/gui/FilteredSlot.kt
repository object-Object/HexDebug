package gay.`object`.hexdebug.gui

import net.minecraft.world.Container
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

class FilteredSlot(
    container: Container,
    slot: Int,
    x: Int,
    y: Int,
    var maxStackSize: Int? = null,
    var mayPlace: ((ItemStack) -> Boolean)? = null,
) : Slot(container, slot, x, y) {
    override fun getMaxStackSize() = maxStackSize ?: super.getMaxStackSize()

    override fun mayPlace(stack: ItemStack) = mayPlace?.invoke(stack) ?: super.mayPlace(stack)
}
