package gay.`object`.hexdebug.blocks.splicing

import at.petrak.hexcasting.api.addldata.ADIotaHolder
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.NullIota
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.blocks.base.BaseContainer
import gay.`object`.hexdebug.blocks.base.ContainerSlotDelegate
import gay.`object`.hexdebug.gui.SplicingTableMenu
import gay.`object`.hexdebug.registry.HexDebugBlockEntities
import gay.`object`.hexdebug.splicing.ISplicingTable
import gay.`object`.hexdebug.splicing.ISplicingTable.Action
import gay.`object`.hexdebug.splicing.ISplicingTable.Companion.CLIPBOARD_INDEX
import gay.`object`.hexdebug.splicing.ISplicingTable.Companion.CONTAINER_SIZE
import gay.`object`.hexdebug.splicing.ISplicingTable.Companion.LIST_INDEX
import gay.`object`.hexdebug.splicing.Selection
import gay.`object`.hexdebug.splicing.SplicingTableClientView
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.ContainerHelper
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class SplicingTableBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(
    HexDebugBlockEntities.SPLICING_TABLE.value, pos, state
), ISplicingTable, BaseContainer, MenuProvider {
    override val stacks = BaseContainer.withSize(CONTAINER_SIZE)

    private var listStack by ContainerSlotDelegate(LIST_INDEX)
    private var clipboardStack by ContainerSlotDelegate(CLIPBOARD_INDEX)

    val analogOutputSignal get() = if (!listStack.isEmpty) 15 else 0

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

    private fun getList(level: ServerLevel) =
        IXplatAbstractions.INSTANCE.findDataHolder(listStack).let { holder ->
            holder to holder?.let { it.readIota(level) as? ListIota }?.list?.toMutableList()
        }

    private fun getClipboard(level: ServerLevel) =
        IXplatAbstractions.INSTANCE.findDataHolder(clipboardStack).let { holder ->
            holder to holder?.readIota(level)
        }

    override fun getClientView(): SplicingTableClientView? {
        val level = level as? ServerLevel ?: return null

        val (listHolder, list) = getList(level)
        val (clipboardHolder, clipboard) = getClipboard(level)

        return SplicingTableClientView(
            iotas = list?.map { IotaType.serialize(it) },
            clipboard = clipboard?.let { IotaType.serialize(it) },
            isWritable = listHolder?.writeIota(ListIota(listOf()), true) ?: false,
            isClipboardWritable = clipboardHolder?.writeIota(NullIota(), true) ?: false,
        )
    }

    /** Called on the server. */
    override fun runAction(action: Action, selection: Selection?): Selection? {
        val level = level as? ServerLevel ?: return selection

        val (listHolder, list) = getList(level)

        when (action) {
            Action.UNDO -> return applyUndoState(-1, selection)
            Action.REDO -> return applyUndoState(1, selection)
            else -> {}
        }

        if (selection == null) return null
        if (listHolder == null || list == null) return selection

        when (action) {
            Action.PASTE -> {

                return Selection.withSize(selection.lastIndex + 1, 1)
            }
            Action.PASTE_SPLAT -> {

                return Selection.withSize(selection.lastIndex + 1, 1) // FIXME: replace 1 with size of clipboard
            }
            else -> {}
        }

        if (selection.end == null) return selection

        @Suppress("KotlinConstantConditions")
        return when (action) {
            Action.NUDGE_LEFT -> {
                if (selection.start > 0) {
                    list.add(selection.end, list.removeAt(selection.start - 1))
                    writeList(listHolder, list)
                    selection.moveBy(-1)
                } else selection
            }
            Action.NUDGE_RIGHT -> {
                if (selection.end < list.lastIndex) {
                    list.add(selection.start - 1, list.removeAt(selection.end + 1))
                    writeList(listHolder, list)
                    selection.moveBy(1)
                } else selection
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
            Action.UNDO, Action.REDO, Action.PASTE, Action.PASTE_SPLAT -> throw AssertionError("unreachable")
        }
    }

    private fun isWritable(holder: ADIotaHolder?, iota: Iota) = holder?.writeIota(iota, true) ?: false

    private fun writeList(holder: ADIotaHolder, list: List<Iota>) {
        holder.writeIota(ListIota(list), false)
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
