package gay.`object`.hexdebug.gui.splicing

import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.api.utils.isMediaItem
import gay.`object`.hexdebug.blocks.base.ContainerDataLongDelegate
import gay.`object`.hexdebug.blocks.splicing.ClientSplicingTableContainer
import gay.`object`.hexdebug.blocks.splicing.SplicingTableDataSlot
import gay.`object`.hexdebug.blocks.splicing.SplicingTableItemSlot
import gay.`object`.hexdebug.gui.BaseContainerMenu
import gay.`object`.hexdebug.gui.FilteredSlot
import gay.`object`.hexdebug.networking.msg.MsgSplicingTableGetDataC2S
import gay.`object`.hexdebug.networking.msg.MsgSplicingTableNewDataS2C
import gay.`object`.hexdebug.registry.HexDebugMenus
import gay.`object`.hexdebug.splicing.ISplicingTable
import gay.`object`.hexdebug.splicing.SplicingTableClientView
import gay.`object`.hexdebug.utils.isIotaHolder
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
        lowIndex = SplicingTableDataSlot.MEDIA_LOW.index,
        highIndex = SplicingTableDataSlot.MEDIA_HIGH.index,
    )

    var clientView = SplicingTableClientView.empty()

    val staffSlot: Slot

    init {
        table.startOpen(player)

        // table
        addTableSlot(SplicingTableItemSlot.LIST, 88, 68) {
            mayPlace = ::isIotaHolder
        }
        addTableSlot(SplicingTableItemSlot.CLIPBOARD, 7, 68) {
            mayPlace = ::isIotaHolder
        }
        addTableSlot(SplicingTableItemSlot.MEDIA, 193, 68) { // FIXME: placeholder
            mayPlace = ::isMediaItem
        }
        staffSlot = addTableSlot(SplicingTableItemSlot.STAFF, 193, 86) { // FIXME: placeholder
            maxStackSize = 1
            mayPlace = { it.`is`(HexTags.Items.STAVES) }
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
        table.getClientView()?.let { MsgSplicingTableNewDataS2C(it).sendToPlayer(player) }
    }

    companion object {
        fun getInstance(player: Player) = player.containerMenu as? SplicingTableMenu
    }
}
