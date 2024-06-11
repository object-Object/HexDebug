package gay.`object`.hexdebug.gui

import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.api.utils.isMediaItem
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.blocks.base.ContainerDataLongDelegate
import gay.`object`.hexdebug.blocks.splicing.ClientSplicingTableContainer
import gay.`object`.hexdebug.networking.msg.MsgSplicingTableGetDataC2S
import gay.`object`.hexdebug.networking.msg.MsgSplicingTableNewDataS2C
import gay.`object`.hexdebug.registry.HexDebugMenus
import gay.`object`.hexdebug.splicing.ISplicingTable
import gay.`object`.hexdebug.splicing.SplicingTableClientView
import gay.`object`.hexdebug.blocks.splicing.SplicingTableDataSlot
import gay.`object`.hexdebug.blocks.splicing.SplicingTableItemSlot
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.*
import net.minecraft.world.item.ItemStack

class SplicingTableMenu(
    containerId: Int,
    private val inventory: Inventory,
    val table: ISplicingTable,
    data: ContainerData,
) : AbstractContainerMenu(HexDebugMenus.SPLICING_TABLE.value, containerId) {
    constructor(containerId: Int, inventory: Inventory) : this(
        containerId,
        inventory,
        ClientSplicingTableContainer(),
        SimpleContainerData(SplicingTableDataSlot.size),
    )

    val player get() = inventory.player
    val level get() = player.level()

    val media by ContainerDataLongDelegate(
        data,
        lowIndex = SplicingTableDataSlot.MEDIA_LOW.index,
        highIndex = SplicingTableDataSlot.MEDIA_HIGH.index,
    )

    var clientView = SplicingTableClientView.empty()

    val staffSlot: Slot

    init {
        table.startOpen(player)

        // FIXME: placeholder slot coordinates

        // table
        addTableSlot(SplicingTableItemSlot.LIST, 80, 35) {
            mayPlace = ::isIotaHolder
        }
        addTableSlot(SplicingTableItemSlot.CLIPBOARD, 26, 35) {
            mayPlace = ::isIotaHolder
        }
        addTableSlot(SplicingTableItemSlot.MEDIA, 158, 0) {
            mayPlace = ::isMediaItem
        }
        staffSlot = addTableSlot(SplicingTableItemSlot.STAFF, 0, 0) {
            maxStackSize = 1
            mayPlace = { it.`is`(HexTags.Items.STAVES) }
        }
        for ((index, x, y) in SplicingTableItemSlot.STORAGE) {
            addTableSlot(index, 140 + x * 18, 17 + y * 18)
        }

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

        addDataSlots(data)

        addSlotListener(object : ContainerListener {
            override fun slotChanged(menu: AbstractContainerMenu, index: Int, stack: ItemStack) {
                when (index) {
                    SplicingTableItemSlot.LIST.index -> {
                        table.listStackChanged(stack)
                        (player as? ServerPlayer)?.let(::sendData)
                    }
                    SplicingTableItemSlot.CLIPBOARD.index -> {
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

    private fun addTableSlot(
        slot: SplicingTableItemSlot,
        x: Int,
        y: Int,
        builder: FilteredSlot.() -> Unit = {},
    ) = addTableSlot(slot.index, x, y, builder)

    private fun addTableSlot(
        slot: Int,
        x: Int,
        y: Int,
        builder: FilteredSlot.() -> Unit = {},
    ) = addSlot(FilteredSlot(table, slot, x, y).also(builder))

    private fun addInventorySlot(slot: Int, x: Int, y: Int) = addSlot(Slot(inventory, slot, x, y))

    private fun isIotaHolder(stack: ItemStack) = IXplatAbstractions.INSTANCE.findDataHolder(stack) != null

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

        if (index < SplicingTableItemSlot.container_size) {
            // from container to inventory
            if (!moveItemStackTo(originalStack, SplicingTableItemSlot.container_size, slots.size, true)) {
                return ItemStack.EMPTY
            }
        } else {
            // from inventory to container
            if (!moveItemStackTo(originalStack, 0, SplicingTableItemSlot.container_size, false)) {
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
