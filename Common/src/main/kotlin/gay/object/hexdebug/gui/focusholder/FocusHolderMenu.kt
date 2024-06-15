package gay.`object`.hexdebug.gui.focusholder

import gay.`object`.hexdebug.gui.BaseContainerMenu
import gay.`object`.hexdebug.gui.FilteredSlot
import gay.`object`.hexdebug.registry.HexDebugBlocks
import gay.`object`.hexdebug.registry.HexDebugMenus
import gay.`object`.hexdebug.utils.isIotaHolder
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory

class FocusHolderMenu(
    containerId: Int,
    inventory: Inventory,
    container: Container,
) : BaseContainerMenu(
    menuType = HexDebugMenus.FOCUS_HOLDER.value,
    containerId = containerId,
    inventory = inventory,
    containerSlots = 1,
) {
    constructor(containerId: Int, inventory: Inventory) : this(containerId, inventory, SimpleContainer(1))

    private val player get() = inventory.player

    init {
        container.startOpen(player)

        addSlot(FilteredSlot(container, 0, 80, 35, maxStackSize = 1) {
            isIotaHolder(it) && !it.`is`(HexDebugBlocks.FOCUS_HOLDER.item)
        })

        // player inventory
        for (y in 0 until 3) {
            for (x in 0 until 9) {
                addInventorySlot(x + y * 9 + 9, 8 + x * 18, 84 + y * 18)
            }
        }

        // player hotbar
        for (x in 0 until 9) {
            addInventorySlot(x, 8 + x * 18, 142)
        }
    }
}
