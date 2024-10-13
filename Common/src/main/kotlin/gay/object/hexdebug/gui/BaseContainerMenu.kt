package gay.`object`.hexdebug.gui

import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

abstract class BaseContainerMenu(
    menuType: MenuType<*>,
    containerId: Int,
    protected val inventory: Inventory,
    protected val containerSlots: Int,
) : AbstractContainerMenu(menuType, containerId) {
    // https://fabricmc.net/wiki/tutorial:screenhandler#screenhandler_and_screen
    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        val slot = slots.getOrNull(index)
            ?.takeIf { it.hasItem() }
            ?: return ItemStack.EMPTY

        val originalStack = slot.item
        val newStack = originalStack.copy()

        if (index < containerSlots) {
            // from container to inventory
            if (!moveItemStackTo(originalStack, containerSlots, slots.size, true)) {
                return ItemStack.EMPTY
            }
        } else {
            // from inventory to container
            if (!moveItemStackTo(originalStack, 0, containerSlots, false)) {
                return ItemStack.EMPTY
            }
        }

        if (originalStack.isEmpty) {
            slot.set(ItemStack.EMPTY)
        } else {
            slot.setChanged()
        }

        return newStack
    }

    override fun stillValid(player: Player) = inventory.stillValid(player)

    protected fun addInventorySlot(slot: Int, x: Int, y: Int): Slot = addSlot(Slot(inventory, slot, x, y))
}
