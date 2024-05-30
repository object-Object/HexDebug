package gay.`object`.hexdebug.gui

import gay.`object`.hexdebug.blocks.splicing.ClientSplicingTable
import gay.`object`.hexdebug.blocks.splicing.ISplicingTable
import gay.`object`.hexdebug.registry.HexDebugMenus
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

class SplicingTableMenu(
    containerId: Int,
    private val inventory: Inventory,
    private val table: ISplicingTable,
) : AbstractContainerMenu(HexDebugMenus.SPLICING_TABLE.value, containerId) {
    constructor(containerId: Int, inventory: Inventory) : this(containerId, inventory, ClientSplicingTable())

    init {
        table.startOpen(inventory.player)

        // TODO: add actual slot coordinates

        // table
        addSlot(Slot(table, ISplicingTable.IOTA_HOLDER_INDEX, 0, 0))
        addSlot(Slot(table, ISplicingTable.CLIPBOARD_INDEX, 0, 18))

        // player inventory
        for (m in 0 until 3) {
            for (l in 0 until 9) {
                addSlot(Slot(inventory, l + m * 9 + 9, 8 + l * 18, 84 + m * 18))
            }
        }

        // player hotbar
        for (m in 0 until 9) {
            addSlot(Slot(inventory, m, 8 + m * 18, 142))
        }
    }

    // https://fabricmc.net/wiki/tutorial:screenhandler#screenhandler_and_screen
    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        val slot = slots.getOrNull(index)
            ?.takeIf { it.hasItem() }
            ?: return ItemStack.EMPTY

        val originalStack = slot.item
        val newStack = originalStack.copy()

        if (index < ISplicingTable.CONTAINER_SIZE) {
            // from container to inventory
            if (!moveItemStackTo(originalStack, ISplicingTable.CONTAINER_SIZE, slots.size, true)) {
                return ItemStack.EMPTY
            }
        } else {
            // from inventory to container
            if (!moveItemStackTo(originalStack, 0, ISplicingTable.CONTAINER_SIZE, false)) {
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
}
