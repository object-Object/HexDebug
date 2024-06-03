package gay.`object`.hexdebug.gui

import gay.`object`.hexdebug.blocks.splicing.ClientSplicingTableContainer
import gay.`object`.hexdebug.networking.msg.MsgSplicingTableGetDataC2S
import gay.`object`.hexdebug.networking.msg.MsgSplicingTableNewDataS2C
import gay.`object`.hexdebug.registry.HexDebugMenus
import gay.`object`.hexdebug.splicing.ISplicingTable
import gay.`object`.hexdebug.splicing.SplicingTableClientView
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerListener
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

class SplicingTableMenu(
    containerId: Int,
    private val inventory: Inventory,
    val table: ISplicingTable,
) : AbstractContainerMenu(HexDebugMenus.SPLICING_TABLE.value, containerId) {
    constructor(containerId: Int, inventory: Inventory) : this(containerId, inventory, ClientSplicingTableContainer())

    val player get() = inventory.player
    val level get() = player.level()

    var clientView = SplicingTableClientView.empty()

    init {
        table.startOpen(player)

        // FIXME: placeholder slot coordinates

        // table
        addSlot(Slot(table, ISplicingTable.LIST_INDEX, 80, 35))
        addSlot(Slot(table, ISplicingTable.CLIPBOARD_INDEX, 26, 35))

        // player inventory
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }

        // player hotbar
        for (col in 0 until 9) {
            addSlot(Slot(inventory, col, 8 + col * 18, 142))
        }

        addSlotListener(object : ContainerListener {
            override fun slotChanged(menu: AbstractContainerMenu, index: Int, stack: ItemStack) {
                when (index) {
                    ISplicingTable.LIST_INDEX -> {
                        table.listStackChanged(stack)
                        (player as? ServerPlayer)?.let(::sendData)
                    }
                    ISplicingTable.CLIPBOARD_INDEX -> {
                        table.clipboardStackChanged(stack)
                        (player as? ServerPlayer)?.let(::sendData)
                    }
                }
            }

            override fun dataChanged(menu: AbstractContainerMenu, index: Int, value: Int) {}
        })

        // when the client menu is ready, ask the server to send the initial client view
        if (level.isClientSide) {
            MsgSplicingTableGetDataC2S().sendToServer()
        }
    }

    fun sendData(player: ServerPlayer) {
        table.getClientView()?.let { MsgSplicingTableNewDataS2C(it).sendToPlayer(player) }
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

    companion object {
        fun getInstance(player: Player) = player.containerMenu as? SplicingTableMenu
    }
}
