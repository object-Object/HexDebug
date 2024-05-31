package gay.`object`.hexdebug.blocks.splicing

import at.petrak.hexcasting.api.casting.iota.Iota
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.blocks.base.BaseContainer
import gay.`object`.hexdebug.blocks.splicing.ISplicingTable.Action
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

    private val undoStack = mutableListOf<UndoState>()
    private var undoIndex = -1

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
    override fun runAction(action: Action, selection: Selection?): Selection? {
        HexDebug.LOGGER.info("Got action: $action") // FIXME: remove

        when (action) {
            Action.UNDO -> return applyUndoState(-1, selection)
            Action.REDO -> return applyUndoState(1, selection)
            else -> {}
        }

        if (selection == null) return null

        @Suppress("KotlinConstantConditions")
        return when (action) {
            Action.NUDGE_LEFT -> {

                selection.nudge(-1)
            }
            Action.NUDGE_RIGHT -> {

                selection.nudge(1)
            }
            Action.DUPLICATE -> {

                selection.expandRight(selection.size)
            }
            Action.DELETE -> {

                null
            }
            Action.CUT -> {

                null
            }
            Action.COPY -> {

                selection
            }
            Action.PASTE -> {

                Selection.withSize(selection.end, 1)
            }
            Action.PASTE_SPLAT -> {

                Selection.withSize(selection.end, 0) // FIXME: replace 0 with size of clipboard
            }
            Action.UNDO, Action.REDO -> throw AssertionError("unreachable")
        }
    }

    private fun applyUndoState(delta: Int, currentSelection: Selection?): Selection? {
        val newIndex = undoIndex + delta
        val state = undoStack.getOrNull(newIndex) ?: return currentSelection
        undoIndex = newIndex
        // FIXME: implement
        return state.selection
    }

    private fun pushUndoState() {}

    data class UndoState(
        val list: List<Iota>,
        val clipboard: List<Iota>?,
        val selection: Selection?,
    )
}
