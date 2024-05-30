package gay.`object`.hexdebug.gui

import gay.`object`.hexdebug.blocks.splicing.ClientSplicingTable
import gay.`object`.hexdebug.blocks.splicing.ISplicingTable
import gay.`object`.hexdebug.registry.HexDebugMenus
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack

class SplicingTableMenu(
    containerId: Int,
    private val inventory: Inventory,
    private val splicingTable: ISplicingTable,
) : AbstractContainerMenu(HexDebugMenus.SPLICING_TABLE.value, containerId) {
    constructor(containerId: Int, inventory: Inventory) : this(containerId, inventory, ClientSplicingTable())

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        TODO("Not yet implemented")
    }

    override fun stillValid(player: Player) = inventory.stillValid(player)
}
