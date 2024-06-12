package gay.`object`.hexdebug.blocks.splicing

import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.utils.extractMedia
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.blocks.base.BaseContainer
import gay.`object`.hexdebug.blocks.base.ContainerDataLongDelegate
import gay.`object`.hexdebug.config.HexDebugConfig
import gay.`object`.hexdebug.gui.splicing.SplicingTableMenu
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
    override val stacks = BaseContainer.withSize(SplicingTableItemSlot.container_size)

    private var listStack by SplicingTableItemSlot.LIST.delegate
    private var clipboardStack by SplicingTableItemSlot.CLIPBOARD.delegate
    private var mediaStack by SplicingTableItemSlot.MEDIA.delegate
    private var staffStack by SplicingTableItemSlot.STAFF.delegate

    private val containerData = SimpleContainerData(SplicingTableDataSlot.size)

    private var media by ContainerDataLongDelegate(
        containerData,
        lowIndex = SplicingTableDataSlot.MEDIA_LOW.index,
        highIndex = SplicingTableDataSlot.MEDIA_HIGH.index,
    )

    val analogOutputSignal get() = if (!listStack.isEmpty) 15 else 0

    private val undoStack = UndoStack()

    override fun load(tag: CompoundTag) {
        super.load(tag)
        ContainerHelper.loadAllItems(tag, stacks)
        media = tag.getLong("media")
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        ContainerHelper.saveAllItems(tag, stacks)
        tag.putLong("media", media)
    }

    override fun createMenu(i: Int, inventory: Inventory, player: Player) =
        SplicingTableMenu(i, inventory, this, containerData)

    override fun getDisplayName() = Component.translatable(blockState.block.descriptionId)

    override fun setChanged() {
        super.setChanged()
        refillMedia()
    }

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
        if (media < mediaCost) return selection
        val data = getData(player, selection) ?: return selection
        if (undoStack.size == 0) {
            data.pushUndoState(
                list = Some(data.list.orEmpty()),
                clipboard = Some(data.clipboard),
                selection = Some(selection),
            )
        }
        return convertAndRun(action.value, data)
    }

    private fun <T : SplicingTableData> convertAndRun(
        actionValue: SplicingTableAction.Value<T>,
        data: SplicingTableData,
    ): Selection? {
        val converted = actionValue.convert(data) ?: return data.selection
        return actionValue.run(converted).also { consumeMedia(converted) }
    }

    override fun drawPattern(
        player: ServerPlayer?,
        pattern: HexPattern,
        index: Int,
        selection: Selection?
    ): Pair<Selection?, ResolvedPatternType> {
        val errorResult = selection to ResolvedPatternType.ERRORED
        if (media < mediaCost) return errorResult
        val data = getData(player, selection)
            ?.let(ReadWriteList::convertOrNull)
            ?: return errorResult
        return drawPattern(pattern, data).also { consumeMedia(data) }
    }

    private fun drawPattern(pattern: HexPattern, data: ReadWriteList) = data.run {
        selection.mutableSubList(list).apply {
            clear()
            add(PatternIota(pattern))
        }
        if (writeList(list)) {
            shouldConsumeMedia = true
            pushUndoState(
                list = Some(list),
                selection = Some(Selection.edge(selection.start + 1)),
            ) to ResolvedPatternType.ESCAPED
        } else {
            selection to ResolvedPatternType.ERRORED
        }
    }

    private fun consumeMedia(data: SplicingTableData) {
        if (data.shouldConsumeMedia) {
            media = (media - mediaCost).coerceIn(0, maxMedia)
            refillMedia()
        }
    }

    private fun refillMedia() {
        val mediaHolder = IXplatAbstractions.INSTANCE.findMediaHolder(mediaStack) ?: return
        while (media < maxMedia) {
            val cost = maxMedia - media

            // avoid wasting media if the item is too large
            if (extractMedia(mediaStack, cost = cost, simulate = true) !in 1..cost) return

            // avoid an infinite loop - stop looping as soon as we stop adding media
            val extracted = extractMedia(mediaHolder, cost = cost)
            if (extracted < 1) return

            media += extracted
        }
    }

    companion object {
        private val config get() = HexDebugConfig.server

        private val mediaCost get() = config.splicingTableMediaCost
        private val maxMedia get() = config.splicingTableMaxMedia.coerceIn(0, null)
    }
}
