package gay.`object`.hexdebug.splicing

import at.petrak.hexcasting.api.addldata.ADIotaHolder
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.mishaps.MishapOthersName
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.utils.Option
import gay.`object`.hexdebug.utils.Option.None
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

interface SplicingTableDataConverter<T : SplicingTableData> {
    fun test(view: SplicingTableClientView, selection: Selection?, viewStartIndex: Int): Boolean

    fun convert(data: SplicingTableData): T

    fun convertOrNull(data: SplicingTableData): T? = try {
        convert(data)
    } catch (e: Throwable) {
        HexDebug.LOGGER.debug("{} failed to convert data {}: {}", this, data, e.stackTraceToString())
        null
    }
}

/** Server-side data used by splicing table action implementations. */
open class SplicingTableData(
    open val player: ServerPlayer?,
    val level: ServerLevel,
    val undoStack: UndoStack,
    var selection: Selection?,
    var viewStartIndex: Int,
    open val list: MutableList<Iota>?,
    open val listWriter: ADIotaHolder?,
    open val clipboard: Iota?,
    open val clipboardWriter: ADIotaHolder?,
    var shouldConsumeMedia: Boolean = false,
) {
    constructor(
        player: ServerPlayer?,
        level: ServerLevel,
        undoStack: UndoStack,
        selection: Selection?,
        viewStartIndex: Int,
        listHolder: ADIotaHolder?,
        clipboardHolder: ADIotaHolder?,
    ) : this(
        player,
        level,
        undoStack,
        selection,
        viewStartIndex,
        list = listHolder?.let { it.readIota(level) as? ListIota }?.list?.toMutableList(),
        listWriter = listHolder?.takeIfWritable(),
        clipboard = clipboardHolder?.readIota(level),
        clipboardWriter = clipboardHolder?.takeIfWritable()
    )

    var viewEndIndex
        get() = viewStartIndex + VIEW_END_INDEX_OFFSET
        set(value) {
            viewStartIndex = value - VIEW_END_INDEX_OFFSET
        }

    fun isInRange(index: Int) = list?.let { index in it.indices } ?: false

    fun pushUndoState(
        list: Option<List<Iota>> = None(),
        clipboard: Option<Iota?> = None(),
        selection: Option<Selection?> = None(),
    ) {
        // copy list to avoid mutability issues
        undoStack.push(UndoStack.Entry(list.map { it.toList() }, clipboard, selection))
    }

    fun writeList(value: List<Iota>) = writeIota(listWriter, ListIota(value))

    fun writeClipboard(value: List<Iota>?) = writeClipboard(value?.let(::ListIota))

    fun writeClipboard(value: Iota?) = writeIota(clipboardWriter, value)

    fun writeIota(holder: ADIotaHolder?, value: Iota?): Boolean {
        if (holder == null || (value != null && IotaType.isTooLargeToSerialize(listOf(value)))) return false
        return holder.writeIota(value, false).also {
            if (it) shouldConsumeMedia = true
        }
    }

    fun isClipboardTransferSafe(value: Iota) = isClipboardTransferSafe(listOf(value))

    // prevent transfer if list contains someone else's truename
    fun isClipboardTransferSafe(value: List<Iota>) = null == MishapOthersName.getTrueNameFromArgs(value, player)

    companion object : SplicingTableDataConverter<SplicingTableData> {
        override fun test(view: SplicingTableClientView, selection: Selection?, viewStartIndex: Int) = true
        override fun convert(data: SplicingTableData) = data
    }
}

open class ReadList(
    override val player: ServerPlayer,
    level: ServerLevel,
    undoStack: UndoStack,
    selection: Selection?,
    viewStartIndex: Int,
    override val list: MutableList<Iota>,
    listWriter: ADIotaHolder?,
    clipboard: Iota?,
    clipboardWriter: ADIotaHolder?,
) : SplicingTableData(player, level, undoStack, selection, viewStartIndex, list, listWriter, clipboard, clipboardWriter) {
    companion object : SplicingTableDataConverter<ReadList> {
        override fun test(view: SplicingTableClientView, selection: Selection?, viewStartIndex: Int) = view.isListReadable

        override fun convert(data: SplicingTableData) = data.run {
            ReadList(
                player!!,
                level,
                undoStack,
                selection,
                viewStartIndex,
                list!!,
                listWriter,
                clipboard,
                clipboardWriter,
            )
        }
    }
}

open class ReadWriteList(
    player: ServerPlayer,
    level: ServerLevel,
    undoStack: UndoStack,
    // hack
    open val typedSelection: Selection,
    viewStartIndex: Int,
    override val list: MutableList<Iota>,
    override val listWriter: ADIotaHolder,
    clipboard: Iota?,
    clipboardWriter: ADIotaHolder?,
) : ReadList(player, level, undoStack, typedSelection, viewStartIndex, list, listWriter, clipboard, clipboardWriter) {
    companion object : SplicingTableDataConverter<ReadWriteList> {
        override fun test(view: SplicingTableClientView, selection: Selection?, viewStartIndex: Int) =
            selection != null && view.run { isListReadable && isListWritable }

        override fun convert(data: SplicingTableData) = data.run {
            ReadWriteList(
                player!!,
                level,
                undoStack,
                selection!!,
                viewStartIndex,
                list!!,
                listWriter!!,
                clipboard,
                clipboardWriter,
            )
        }
    }
}

class ReadWriteListFromClipboard(
    player: ServerPlayer,
    level: ServerLevel,
    undoStack: UndoStack,
    typedSelection: Selection,
    viewStartIndex: Int,
    list: MutableList<Iota>,
    listWriter: ADIotaHolder,
    override val clipboard: Iota,
    clipboardWriter: ADIotaHolder?,
) : ReadWriteList(player, level, undoStack, typedSelection, viewStartIndex, list, listWriter, clipboard, clipboardWriter) {
    companion object : SplicingTableDataConverter<ReadWriteListFromClipboard> {
        override fun test(view: SplicingTableClientView, selection: Selection?, viewStartIndex: Int) =
            selection != null && view.run { isListReadable && isListWritable && isClipboardReadable }

        override fun convert(data: SplicingTableData) = data.run {
            ReadWriteListFromClipboard(
                player!!,
                level,
                undoStack,
                selection!!,
                viewStartIndex,
                list!!,
                listWriter!!,
                clipboard!!,
                clipboardWriter,
            )
        }
    }
}

open class ReadWriteListRange(
    player: ServerPlayer,
    level: ServerLevel,
    undoStack: UndoStack,
    override val typedSelection: Selection.Range,
    viewStartIndex: Int,
    list: MutableList<Iota>,
    listWriter: ADIotaHolder,
    clipboard: Iota?,
    clipboardWriter: ADIotaHolder?,
) : ReadWriteList(player, level, undoStack, typedSelection, viewStartIndex, list, listWriter, clipboard, clipboardWriter) {
    companion object : SplicingTableDataConverter<ReadWriteListRange> {
        override fun test(view: SplicingTableClientView, selection: Selection?, viewStartIndex: Int) =
            selection is Selection.Range && view.run { isListReadable && isListWritable }

        override fun convert(data: SplicingTableData) = data.run {
            ReadWriteListRange(
                player!!,
                level,
                undoStack,
                selection as Selection.Range,
                viewStartIndex,
                list!!,
                listWriter!!,
                clipboard,
                clipboardWriter,
            )
        }
    }
}

class ReadWriteListRangeToClipboard(
    override val player: ServerPlayer,
    level: ServerLevel,
    undoStack: UndoStack,
    typedSelection: Selection.Range,
    viewStartIndex: Int,
    list: MutableList<Iota>,
    listWriter: ADIotaHolder,
    clipboard: Iota?,
    override val clipboardWriter: ADIotaHolder,
) : ReadWriteListRange(player, level, undoStack, typedSelection, viewStartIndex, list, listWriter, clipboard, clipboardWriter) {
    companion object : SplicingTableDataConverter<ReadWriteListRangeToClipboard> {
        override fun test(view: SplicingTableClientView, selection: Selection?, viewStartIndex: Int) =
            selection is Selection.Range && view.run { isListReadable && isListWritable && isClipboardWritable }

        override fun convert(data: SplicingTableData) = data.run {
            ReadWriteListRangeToClipboard(
                player!!,
                level,
                undoStack,
                selection as Selection.Range,
                viewStartIndex,
                list!!,
                listWriter!!,
                clipboard,
                clipboardWriter!!,
            )
        }
    }
}

class ReadListRangeToClipboard(
    player: ServerPlayer,
    level: ServerLevel,
    undoStack: UndoStack,
    val typedSelection: Selection.Range,
    viewStartIndex: Int,
    override val list: MutableList<Iota>,
    listWriter: ADIotaHolder?,
    clipboard: Iota?,
    override val clipboardWriter: ADIotaHolder,
) : ReadList(player, level, undoStack, typedSelection, viewStartIndex, list, listWriter, clipboard, clipboardWriter) {
    companion object : SplicingTableDataConverter<ReadListRangeToClipboard> {
        override fun test(view: SplicingTableClientView, selection: Selection?, viewStartIndex: Int) =
            selection is Selection.Range && view.run { isListReadable && isClipboardWritable }

        override fun convert(data: SplicingTableData) = data.run {
            ReadListRangeToClipboard(
                player!!,
                level,
                undoStack,
                selection as Selection.Range,
                viewStartIndex,
                list!!,
                listWriter,
                clipboard,
                clipboardWriter!!,
            )
        }
    }
}

private fun ADIotaHolder.takeIfWritable() = takeIf { writeable() }
