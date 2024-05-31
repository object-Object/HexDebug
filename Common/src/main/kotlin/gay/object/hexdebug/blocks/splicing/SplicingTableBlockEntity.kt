package gay.`object`.hexdebug.blocks.splicing

import gay.`object`.hexdebug.blocks.base.BaseContainer
import gay.`object`.hexdebug.blocks.splicing.ISplicingTable.Action
import gay.`object`.hexdebug.blocks.splicing.ISplicingTable.Selection
import gay.`object`.hexdebug.gui.SplicingTableMenu
import gay.`object`.hexdebug.registry.HexDebugBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.ContainerHelper
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class SplicingTableBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(
    HexDebugBlockEntities.SPLICING_TABLE.value, pos, state
), ISplicingTable, BaseContainer, MenuProvider {
    override val stacks = BaseContainer.withSize(ISplicingTable.CONTAINER_SIZE)

    override var iotaHolder by iotaHolderDelegate()
    override var clipboard by clipboardDelegate()

    val analogOutputSignal get() = if (!iotaHolder.isEmpty) 15 else 0

    override fun load(tag: CompoundTag) {
        super.load(tag)
        ContainerHelper.loadAllItems(tag, stacks)
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        ContainerHelper.saveAllItems(tag, stacks)
    }

    override fun createMenu(i: Int, inventory: Inventory, player: Player) = SplicingTableMenu(i, inventory, this)

    override fun getDisplayName() = Component.translatable(blockState.block.descriptionId)

    /** Called on the server. */
    override fun runAction(action: Action, selection: Selection): Selection? {
        when (action) {
            Action.NUDGE_LEFT -> {

            }
            Action.NUDGE_RIGHT -> {

            }
            Action.DUPLICATE -> {

            }
            Action.DELETE -> {

            }
            Action.UNDO -> {

            }
            Action.REDO -> {

            }
            Action.CUT -> {

            }
            Action.COPY -> {

            }
            Action.PASTE -> {

            }
            Action.PASTE_SPLAT -> {

            }
        }
        return selection
    }
}
