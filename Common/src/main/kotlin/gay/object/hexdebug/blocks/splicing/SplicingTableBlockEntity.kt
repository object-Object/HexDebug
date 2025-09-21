package gay.`object`.hexdebug.blocks.splicing

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.addldata.ADIotaHolder
import at.petrak.hexcasting.api.block.HexBlockEntity
import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.pigment.FrozenPigment
import at.petrak.hexcasting.api.utils.asCompound
import at.petrak.hexcasting.api.utils.extractMedia
import at.petrak.hexcasting.api.utils.getInt
import at.petrak.hexcasting.api.utils.getList
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.blocks.base.BaseContainer
import gay.`object`.hexdebug.blocks.base.ContainerDataDelegate
import gay.`object`.hexdebug.blocks.base.ContainerDataLongDelegate
import gay.`object`.hexdebug.blocks.base.ContainerDataSelectionDelegate
import gay.`object`.hexdebug.casting.eval.FakeCastEnv
import gay.`object`.hexdebug.casting.eval.SplicingTableCastEnv
import gay.`object`.hexdebug.config.HexDebugServerConfig
import gay.`object`.hexdebug.gui.splicing.SplicingTableMenu
import gay.`object`.hexdebug.registry.HexDebugBlockEntities
import gay.`object`.hexdebug.splicing.*
import gay.`object`.hexdebug.utils.Option.None
import gay.`object`.hexdebug.utils.Option.Some
import gay.`object`.hexdebug.utils.setPropertyIfChanged
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.ContainerHelper
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import kotlin.math.max
import kotlin.math.min

class SplicingTableBlockEntity(pos: BlockPos, state: BlockState) :
    HexBlockEntity(HexDebugBlockEntities.SPLICING_TABLE.value, pos, state),
    ISplicingTable, BaseContainer, MenuProvider, ADIotaHolder
{
    override val stacks = BaseContainer.withSize(SplicingTableItemSlot.container_size)

    var listStack by SplicingTableItemSlot.LIST.delegate
    var clipboardStack by SplicingTableItemSlot.CLIPBOARD.delegate
    var mediaStack by SplicingTableItemSlot.MEDIA.delegate
        private set
    private var staffStack by SplicingTableItemSlot.STAFF.delegate

    private val listHolder get() = IXplatAbstractions.INSTANCE.findDataHolder(listStack)
    val clipboardHolder get() = IXplatAbstractions.INSTANCE.findDataHolder(clipboardStack)

    private val containerData = SimpleContainerData(SplicingTableDataSlot.size)

    var media by ContainerDataLongDelegate(
        containerData,
        index0 = SplicingTableDataSlot.MEDIA_0.index,
        index1 = SplicingTableDataSlot.MEDIA_1.index,
        index2 = SplicingTableDataSlot.MEDIA_2.index,
        index3 = SplicingTableDataSlot.MEDIA_3.index,
    )

    var selection by ContainerDataSelectionDelegate(
        containerData,
        fromIndex = SplicingTableDataSlot.SELECTION_FROM.index,
        toIndex = SplicingTableDataSlot.SELECTION_TO.index,
    )
        private set

    var viewStartIndex by ContainerDataDelegate(
        containerData,
        index = SplicingTableDataSlot.VIEW_START_INDEX.index,
    )
        private set

    private var castingCooldown = 0

    val analogOutputSignal get() = if (!listStack.isEmpty) 15 else 0

    // TODO: save?
    private val undoStack = UndoStack()

    private var hexTag: ListTag? = null

    var pigment: FrozenPigment? = null

    val enlightened get() = (blockState.block as? SplicingTableBlock)?.enlightened ?: false

    fun getList(level: ServerLevel) =
        listHolder?.let { it.readIota(level) as? ListIota }?.list

    // for use by actions
    // can't be called setSelection because that conflicts with the property setter
    fun writeSelection(selection: Selection?) {
        this.selection = selection
        clampView(level)
    }

    fun writeViewStartIndex(viewStartIndex: Int) {
        this.viewStartIndex = viewStartIndex
        clampView(level)
    }

    override fun loadModData(tag: CompoundTag) {
        ContainerHelper.loadAllItems(tag, stacks)
        media = tag.getLong("media")
        selection = Selection.fromRawIndices(
            from = tag.getInt("selectionFrom", -1),
            to = tag.getInt("selectionTo", -1),
        )
        viewStartIndex = tag.getInt("viewStartIndex")
        hexTag = if (tag.contains("hex")) tag.getList("hex", Tag.TAG_COMPOUND) else null
        pigment = if (tag.contains("pigment")) FrozenPigment.fromNBT(tag.getCompound("pigment")) else null
    }

    override fun saveModData(tag: CompoundTag) {
        ContainerHelper.saveAllItems(tag, stacks)
        tag.putLong("media", media)
        tag.putInt("selectionFrom", selection?.from ?: -1)
        tag.putInt("selectionTo", selection?.to ?: -1)
        tag.putInt("viewStartIndex", viewStartIndex)
        hexTag?.let { tag.put("hex", it) }
        pigment?.let { tag.put("pigment", it.serializeToNBT()) }
    }

    override fun createMenu(i: Int, inventory: Inventory, player: Player) =
        SplicingTableMenu(i, inventory, this, containerData)

    override fun getDisplayName() = Component.translatable(blockState.block.descriptionId)

    override fun setChanged() {
        super.setChanged()
        refillMedia()
        setPropertyIfChanged(SplicingTableBlock.IMBUED, hexTag != null)
    }

    /** Only returns null if it fails to convert `this.level` to [ServerLevel]. */
    private fun getData(player: ServerPlayer?): SplicingTableData? {
        return SplicingTableData(
            player = player,
            level = level as? ServerLevel ?: return null,
            undoStack = undoStack,
            selection = selection,
            viewStartIndex = viewStartIndex,
            listHolder = listHolder,
            clipboardHolder = clipboardHolder,
        )
    }

    private fun setupUndoStack(data: SplicingTableData) {
        if (undoStack.size == 0) {
            data.pushUndoState(
                list = Some(data.list.orEmpty()),
                clipboard = Some(data.clipboard),
                selection = Some(selection),
            )
        }
    }

    override fun getClientView() = getData(null)?.run {
        val env = FakeCastEnv(level)
        SplicingTableClientView(
            list = list?.map { IotaClientView(it, env) },
            clipboard = clipboard?.let { IotaType.serialize(it) },
            isListWritable = listWriter != null,
            isClipboardWritable = clipboardWriter != null,
            isEnlightened = enlightened,
            hasHex = hexTag != null,
            undoSize = undoStack.size,
            undoIndex = undoStack.index,
        )
    }

    override fun listStackChanged(stack: ItemStack) {
        // when the list item is removed, clear the undo stack, selection, and view index
        if (stack.isEmpty) {
            undoStack.clear()
            selection = null
            viewStartIndex = 0
        } else {
            clampView(level)
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

    override fun runAction(action: SplicingTableAction, player: ServerPlayer?) {
        if (action.value.consumesMedia && media < mediaCost) return
        val data = getData(player) ?: return
        clampView(data.level, data)
        setupUndoStack(data)
        convertAndRun(action.value, data)
    }

    private fun <T : SplicingTableData> convertAndRun(
        actionValue: SplicingTableAction.Value<T>,
        data: SplicingTableData,
    ) {
        val converted = actionValue.convert(data) ?: return
        actionValue.run(converted)
        postRunAction(converted)
    }

    override fun drawPattern(player: ServerPlayer?, pattern: HexPattern, index: Int): ResolvedPatternType {
        if (media < mediaCost) return ResolvedPatternType.ERRORED
        val data = getData(player)
            ?.let(ReadWriteList::convertOrNull)
            ?: return ResolvedPatternType.ERRORED
        clampView(data.level, data)
        setupUndoStack(data)
        val result = drawPattern(pattern, data)
        postRunAction(data)
        return result
    }

    private fun drawPattern(pattern: HexPattern, data: ReadWriteList) = data.run {
        typedSelection.mutableSubList(list).apply {
            clear()
            add(PatternIota(pattern))
        }
        if (writeList(list)) {
            shouldConsumeMedia = true
            selection = Selection.edge(typedSelection.start + 1)?.also {
                makeEdgeVisible(it.index)
            }
            pushUndoState(
                list = Some(list),
                selection = Some(selection),
            )
            ResolvedPatternType.ESCAPED
        } else {
            ResolvedPatternType.ERRORED
        }
    }

    override fun selectIndex(player: ServerPlayer?, index: Int, hasShiftDown: Boolean, isIota: Boolean) {
        // scuffed: we don't really need the player here, but the code assumes it's present
        val data = getData(player)
            ?.let(ReadList::convertOrNull)
            ?: return

        if (isIota) {
            selectIota(data, index, hasShiftDown)
        } else {
            selectEdge(data, index, hasShiftDown)
        }
    }

    private fun selectIota(data: ReadList, index: Int, hasShiftDown: Boolean) {
        if (!isInRange(data.list, index)) return

        val selection = selection
        this.selection = if (isOnlyIotaSelected(index)) {
            null
        } else if (hasShiftDown && selection != null) {
            if (selection is Selection.Edge && index < selection.from) {
                Selection.of(selection.from - 1, index)
            } else {
                Selection.of(selection.from, index)
            }
        } else {
            Selection.withSize(index, 1)
        }
    }

    private fun selectEdge(data: ReadList, index: Int, hasShiftDown: Boolean) {
        if (!isEdgeInRange(data.list, index)) return

        val selection = selection
        this.selection = if (isEdgeSelected(index)) {
            null
        } else if (hasShiftDown && selection != null) {
            if (selection is Selection.Edge && index < selection.from) {
                Selection.of(selection.from - 1, index)
            } else if (index > selection.from) {
                Selection.of(selection.from, index - 1)
            } else {
                Selection.of(selection.from, index)
            }
        } else {
            Selection.edge(index)
        }
    }

    private fun isOnlyIotaSelected(index: Int) =
        selection?.let { it.size == 1 && it.from == index } ?: false

    private fun isEdgeSelected(index: Int) =
        selection?.let { it.start == index && it.end == null } ?: false

    private fun isInRange(list: List<Iota>?, index: Int) =
        list?.let { index in it.indices } ?: false

    private fun isEdgeInRange(list: List<Iota>, index: Int) =
        // cell to the right is in range
        isInRange(list, index)
            // cell to the left is in range
            || isInRange(list, index - 1)
            // allow selecting leftmost edge of empty list
            || (index == 0 && list.isEmpty())

    override fun castHex(player: ServerPlayer?) {
        if (
            player == null
            || !enlightened
            || media < mediaCost
            || castingCooldown > 0
        ) return

        val level = level as? ServerLevel ?: return

        val instrs = getHex(level) ?: return

        // consume media first, before the hex has a chance to get at it
        consumeMedia()

        val env = SplicingTableCastEnv(player, this)
        val vm = CastingVM.empty(env)

        val clientView = vm.queueExecuteAndWrapIotas(instrs, level)

        if (clientView.resolutionType.success) {
            ParticleSpray(blockPos.center, Vec3(0.0, 1.5, 0.0), 0.4, Math.PI / 3, 30)
                .sprayParticles(level, env.pigment)
        }

        castingCooldown = config.splicingTableCastingCooldown
    }

    fun getHex(level: ServerLevel): List<Iota>? =
        hexTag?.map { IotaType.deserialize(it.asCompound, level) }

    fun setHex(hex: List<Iota>?) {
        hexTag = hex?.asSequence()
            ?.map { IotaType.serialize(it) }
            ?.toCollection(ListTag())
    }

    private fun postRunAction(data: SplicingTableData) {
        selection = data.selection
        viewStartIndex = data.viewStartIndex
        clampView(data.level, data)

        if (data.shouldConsumeMedia) {
            consumeMedia()
        }
    }

    private fun clampView(level: Level?) {
        (level as? ServerLevel)?.let { clampView(it, null) }
    }

    private fun clampView(level: ServerLevel, data: SplicingTableData?) {
        val list = if (data != null) {
            data.list
        } else {
            getList(level)?.toMutableList()
        }

        if (list != null) {
            val lastIndex = list.lastIndex
            val maxStartIndex = max(0, lastIndex - VIEW_END_INDEX_OFFSET)

            // TODO: gracefully degrade rather than just wiping the whole selection?
            when (val selection = selection) {
                is Selection.Range -> if (!isInRange(list, selection.end)) {
                    this.selection = null
                }
                is Selection.Edge -> if (!isEdgeInRange(list, selection.index)) {
                    this.selection = null
                }
                null -> {}
            }

            viewStartIndex = viewStartIndex.coerceIn(0, maxStartIndex)
        } else {
            selection = null
            viewStartIndex = 0
        }
    }

    private fun consumeMedia() {
        media = (media - mediaCost).coerceIn(0, maxMedia)
        refillMedia()
    }

    fun refillMedia() {
        val mediaHolder = HexAPI.instance().findMediaHolder(mediaStack) ?: return
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

    override fun readIotaTag() = listHolder?.readIotaTag()

    override fun writeIota(iota: Iota?, simulate: Boolean): Boolean {
        val success = listHolder?.writeIota(iota, simulate) ?: false
        if (!simulate && success) {
            sync()
        }
        return success
    }

    override fun writeable() = listHolder?.writeable() ?: false

    companion object {
        private val config get() = HexDebugServerConfig.config

        private val mediaCost get() = config.splicingTableMediaCost
        private val maxMedia get() = config.splicingTableMaxMedia.coerceIn(1, null)

        @Suppress("UNUSED_PARAMETER")
        fun tickServer(level: Level, pos: BlockPos, state: BlockState, blockEntity: SplicingTableBlockEntity) {
            if (blockEntity.castingCooldown > 0) {
                blockEntity.castingCooldown -= 1
            }
        }
    }
}
