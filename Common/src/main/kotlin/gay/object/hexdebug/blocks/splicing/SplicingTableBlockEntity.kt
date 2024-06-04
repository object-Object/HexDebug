package gay.`object`.hexdebug.blocks.splicing

import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.blocks.base.BaseContainer
import gay.`object`.hexdebug.gui.SplicingTableMenu
import gay.`object`.hexdebug.registry.HexDebugBlockEntities
import gay.`object`.hexdebug.splicing.*
import gay.`object`.hexdebug.utils.Option.None
import gay.`object`.hexdebug.utils.Option.Some
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.ContainerHelper
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import kotlin.math.min

class SplicingTableBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(
    HexDebugBlockEntities.SPLICING_TABLE.value, pos, state
), ISplicingTable, BaseContainer, MenuProvider {
    override val stacks = BaseContainer.withSize(SplicingTableSlot.container_size)

    private var listStack by SplicingTableSlot.LIST.delegate
    private var clipboardStack by SplicingTableSlot.CLIPBOARD.delegate
    private var fuelStack by SplicingTableSlot.FUEL.delegate
    private var staffStack by SplicingTableSlot.STAFF.delegate

    private val containerData = SimpleContainerData(SplicingTableDataSlot.size)

    private var media by SplicingTableDataSlot.MEDIA.delegate(containerData)

    val analogOutputSignal get() = if (!listStack.isEmpty) 15 else 0

    private val undoStack = UndoStack()

    override fun load(tag: CompoundTag) {
        super.load(tag)
        ContainerHelper.loadAllItems(tag, stacks)
        media = tag.getInt("media")
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        ContainerHelper.saveAllItems(tag, stacks)
        tag.putInt("media", media)
    }

    override fun createMenu(i: Int, inventory: Inventory, player: Player) =
        SplicingTableMenu(i, inventory, this, containerData)

    override fun getDisplayName() = Component.translatable(blockState.block.descriptionId)

    /** Only returns null if it fails to convert `this.level` to [ServerLevel]. */
    private fun getData(player: ServerPlayer?, selection: Selection?): SplicingTableData? {
        return SplicingTableData(
            player = player,
            level = level as? ServerLevel ?: return null,
            undoStack = undoStack,
            selection = selection,
            listHolder = IXplatAbstractions.INSTANCE.findDataHolder(listStack),
            clipboardHolder = IXplatAbstractions.INSTANCE.findDataHolder(clipboardStack),
        )
    }

    override fun getClientView() = getData(null, null)?.run {
        SplicingTableClientView(
            list = list?.map { IotaType.serialize(it) },
            clipboard = clipboard?.let { IotaType.serialize(it) },
            isListWritable = listWriter != null,
            isClipboardWritable = clipboardWriter != null,
            undoSize = undoStack.size,
            undoIndex = undoStack.index,
        )
    }

    override fun listStackChanged(stack: ItemStack) {
        // when the list item is removed, clear the undo stack
        if (stack.isEmpty) {
            undoStack.clear()
        }
    }

    override fun clipboardStackChanged(stack: ItemStack) {
        // when the clipboard item is removed, remove clipboard changes from all undo stack entries
        if (stack.isEmpty) {
            undoStack.stack.apply {
                val newStack = mapNotNull { entry -> entry.copy(clipboard = None()).takeIf { it.isNotEmpty } }
                clear()
                addAll(newStack)
                undoStack.index = min(undoStack.index, lastIndex)
            }
        }
    }

    override fun runAction(action: SplicingTableAction, player: ServerPlayer?, selection: Selection?): Selection? {
        val data = getData(player, selection) ?: return selection
        if (undoStack.size == 0) {
            data.pushUndoState(
                list = Some(data.list.orEmpty()),
                clipboard = Some(data.clipboard),
                selection = Some(selection),
            )
        }
        return action.value.convertAndRun(data)
    }

    override fun drawPattern(player: ServerPlayer?, pattern: HexPattern, index: Int, selection: Selection?) =
        getData(player, selection)
            ?.let(ReadWriteList::convertOrNull)
            ?.let { drawPattern(pattern, it) }
            ?: (selection to ResolvedPatternType.ERRORED)

    private fun drawPattern(pattern: HexPattern, data: ReadWriteList) = data.run {
        selection.mutableSubList(list).apply {
            clear()
            add(PatternIota(pattern))
        }
        if (writeList(list)) {
            pushUndoState(
                list = Some(list),
                selection = Some(Selection.edge(selection.start + 1)),
            ) to ResolvedPatternType.ESCAPED
        } else {
            null
        }
    }
}
