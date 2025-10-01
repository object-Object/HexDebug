package gay.`object`.hexdebug.gui.splicing

import gay.`object`.hexdebug.blocks.base.ContainerDataDelegate
import gay.`object`.hexdebug.blocks.base.ContainerDataLongDelegate
import gay.`object`.hexdebug.blocks.base.ContainerDataSelectionDelegate
import gay.`object`.hexdebug.blocks.splicing.ClientSplicingTableContainer
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity
import gay.`object`.hexdebug.blocks.splicing.SplicingTableDataSlot
import gay.`object`.hexdebug.blocks.splicing.SplicingTableItemSlot
import gay.`object`.hexdebug.gui.BaseContainerMenu
import gay.`object`.hexdebug.gui.FilteredSlot
import gay.`object`.hexdebug.networking.msg.MsgSplicingTableGetDataC2S
import gay.`object`.hexdebug.networking.msg.MsgSplicingTableNewDataS2C
import gay.`object`.hexdebug.registry.HexDebugMenus
import gay.`object`.hexdebug.splicing.ISplicingTable
import gay.`object`.hexdebug.splicing.SplicingTableClientView
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.*
import net.minecraft.world.item.ItemStack

class SplicingTableMenu(
    containerId: Int,
    inventory: Inventory,
    val table: ISplicingTable,
    data: ContainerData,
) : BaseContainerMenu(
    menuType = HexDebugMenus.SPLICING_TABLE.value,
    containerId = containerId,
    inventory = inventory,
    containerSlots = SplicingTableItemSlot.container_size,
) {
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
        index0 = SplicingTableDataSlot.MEDIA_0.index,
        index1 = SplicingTableDataSlot.MEDIA_1.index,
        index2 = SplicingTableDataSlot.MEDIA_2.index,
        index3 = SplicingTableDataSlot.MEDIA_3.index,
    )

    var selection by ContainerDataSelectionDelegate(
        data,
        fromIndex = SplicingTableDataSlot.SELECTION_FROM.index,
        toIndex = SplicingTableDataSlot.SELECTION_TO.index,
    )
        private set

    var viewStartIndex by ContainerDataDelegate(
        data,
        index = SplicingTableDataSlot.VIEW_START_INDEX.index,
    )
        private set

    var clientView = SplicingTableClientView.empty()

    val mediaSlot: Slot
    val staffSlot: Slot

    init {
        table.startOpen(player)

        // table
        addTableSlot(SplicingTableItemSlot.LIST, 88, 68) {
            mayPlace = SplicingTableBlockEntity.Companion::isValidList
        }
        addTableSlot(SplicingTableItemSlot.CLIPBOARD, 7, 68) {
            mayPlace = SplicingTableBlockEntity.Companion::isValidClipboard
        }
        mediaSlot = addTableSlot(SplicingTableItemSlot.MEDIA, 205, 169) {
            mayPlace = SplicingTableBlockEntity.Companion::isValidMedia
        }
        staffSlot = addTableSlot(SplicingTableItemSlot.STAFF, -20, 169) {
            maxStackSize = 1
            mayPlace = SplicingTableBlockEntity.Companion::isValidStaff
        }
        for ((index, x, y) in SplicingTableItemSlot.STORAGE) {
            addTableSlot(index, 196 + x * 18, 111 + y * 18)
        }

        // player inventory
        for (y in 0 until 3) {
            for (x in 0 until 9) {
                addInventorySlot(x + y * 9 + 9, 16 + x * 18, 111 + y * 18)
            }
        }

        // player hotbar
        for (x in 0 until 9) {
            addInventorySlot(x, 16 + x * 18, 169)
        }

        addDataSlots(data)

        // note: it seems like this ONLY runs on the server?
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

    fun sendData(player: ServerPlayer) {
        table.getClientView()?.let {
            MsgSplicingTableNewDataS2C(it, selection, viewStartIndex).sendToPlayer(player)
        }
    }

    fun receiveData(msg: MsgSplicingTableNewDataS2C) {
        clientView = msg.data
        // TODO: does this make the client send a packet to the server?
        selection = msg.selection
        viewStartIndex = msg.viewStartIndex
    }

    companion object {
        fun getInstance(player: Player) = player.containerMenu as? SplicingTableMenu
    }
}
