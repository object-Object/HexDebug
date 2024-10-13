package gay.`object`.hexdebug.splicing

import at.petrak.hexcasting.api.addldata.ADIotaHolder
import at.petrak.hexcasting.api.spell.iota.Iota
import at.petrak.hexcasting.api.spell.iota.ListIota
import at.petrak.hexcasting.api.spell.mishaps.MishapOthersName.Companion.getTrueNameFromDatum
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.utils.Option
import gay.`object`.hexdebug.utils.Option.None
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

interface SplicingTableDataConverter<T : SplicingTableData> {
    fun test(view: SplicingTableClientView, selection: Selection?): Boolean

    fun convert(data: SplicingTableData): T

    fun convertOrNull(data: SplicingTableData): T? = try {
        convert(data)
    } catch (e: Throwable) {
        HexDebug.LOGGER.debug("{} failed to convert data {}: {}", this, data, e.stackTraceToString())
        null
    }
}

open class SplicingTableData(
    open val player: ServerPlayer?,
    val level: ServerLevel,
    val undoStack: UndoStack,
    open val selection: Selection?,
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
        listHolder: ADIotaHolder?,
        clipboardHolder: ADIotaHolder?,
    ) : this(
        player,
        level,
        undoStack,
        selection,
        list = listHolder?.let { it.readIota(level) as? ListIota }?.list?.toMutableList(),
        listWriter = listHolder?.takeIfWritable(),
        clipboard = clipboardHolder?.readIota(level),
        clipboardWriter = clipboardHolder?.takeIfWritable()
    )

    fun pushUndoState(
        list: Option<List<Iota>> = None(),
        clipboard: Option<Iota?> = None(),
        selection: Option<Selection?> = None(),
    ): Selection? {
        // copy list to avoid mutability issues
        undoStack.push(UndoStack.Entry(list.map { it.toList() }, clipboard, selection))
        return selection.getOrNull()
    }

    fun writeList(value: List<Iota>) = writeIota(listWriter, ListIota(value))

    fun writeClipboard(value: List<Iota>?) = writeClipboard(value?.let(::ListIota))

    fun writeClipboard(value: Iota?) = writeIota(clipboardWriter, value)

    fun writeIota(holder: ADIotaHolder?, value: Iota?): Boolean {
        if (holder == null || (value != null && HexIotaTypes.isTooLargeToSerialize(listOf(value)))) return false
        return holder.writeIota(value, false).also {
            if (it) shouldConsumeMedia = true
        }
    }

    fun isClipboardTransferSafe(value: Iota) = isClipboardTransferSafe(listOf(value))

    // prevent transfer if list contains someone else's truename
    fun isClipboardTransferSafe(value: List<Iota>) = null == value.firstNotNullOfOrNull { getTrueNameFromDatum(it, player) }

    companion object : SplicingTableDataConverter<SplicingTableData> {
        override fun test(view: SplicingTableClientView, selection: Selection?) = true
        override fun convert(data: SplicingTableData) = data
    }
}

open class ReadList(
    override val player: ServerPlayer,
    level: ServerLevel,
    undoStack: UndoStack,
    selection: Selection?,
    override val list: MutableList<Iota>,
    listWriter: ADIotaHolder?,
    clipboard: Iota?,
    clipboardWriter: ADIotaHolder?,
) : SplicingTableData(player, level, undoStack, selection, list, listWriter, clipboard, clipboardWriter) {
    companion object : SplicingTableDataConverter<ReadList> {
        override fun test(view: SplicingTableClientView, selection: Selection?) = view.isListReadable

        override fun convert(data: SplicingTableData) = data.run {
            ReadList(
                player!!,
                level,
                undoStack,
                selection,
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
    override val selection: Selection,
    override val list: MutableList<Iota>,
    override val listWriter: ADIotaHolder,
    clipboard: Iota?,
    clipboardWriter: ADIotaHolder?,
) : ReadList(player, level, undoStack, selection, list, listWriter, clipboard, clipboardWriter) {
    companion object : SplicingTableDataConverter<ReadWriteList> {
        override fun test(view: SplicingTableClientView, selection: Selection?) =
            selection != null && view.run { isListReadable && isListWritable }

        override fun convert(data: SplicingTableData) = data.run {
            ReadWriteList(
                player!!,
                level,
                undoStack,
                selection!!,
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
    selection: Selection,
    list: MutableList<Iota>,
    listWriter: ADIotaHolder,
    override val clipboard: Iota,
    clipboardWriter: ADIotaHolder?,
) : ReadWriteList(player, level, undoStack, selection, list, listWriter, clipboard, clipboardWriter) {
    companion object : SplicingTableDataConverter<ReadWriteListFromClipboard> {
        override fun test(view: SplicingTableClientView, selection: Selection?) =
            selection != null && view.run { isListReadable && isListWritable && isClipboardReadable }

        override fun convert(data: SplicingTableData) = data.run {
            ReadWriteListFromClipboard(
                player!!,
                level,
                undoStack,
                selection!!,
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
    override val selection: Selection.Range,
    list: MutableList<Iota>,
    listWriter: ADIotaHolder,
    clipboard: Iota?,
    clipboardWriter: ADIotaHolder?,
) : ReadWriteList(player, level, undoStack, selection, list, listWriter, clipboard, clipboardWriter) {
    companion object : SplicingTableDataConverter<ReadWriteListRange> {
        override fun test(view: SplicingTableClientView, selection: Selection?) =
            selection is Selection.Range && view.run { isListReadable && isListWritable }

        override fun convert(data: SplicingTableData) = data.run {
            ReadWriteListRange(
                player!!,
                level,
                undoStack,
                selection as Selection.Range,
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
    selection: Selection.Range,
    list: MutableList<Iota>,
    listWriter: ADIotaHolder,
    clipboard: Iota?,
    override val clipboardWriter: ADIotaHolder,
) : ReadWriteListRange(player, level, undoStack, selection, list, listWriter, clipboard, clipboardWriter) {
    companion object : SplicingTableDataConverter<ReadWriteListRangeToClipboard> {
        override fun test(view: SplicingTableClientView, selection: Selection?) =
            selection is Selection.Range && view.run { isListReadable && isListWritable && isClipboardWritable }

        override fun convert(data: SplicingTableData) = data.run {
            ReadWriteListRangeToClipboard(
                player!!,
                level,
                undoStack,
                selection as Selection.Range,
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
    override val selection: Selection.Range,
    override val list: MutableList<Iota>,
    listWriter: ADIotaHolder?,
    clipboard: Iota?,
    override val clipboardWriter: ADIotaHolder,
) : ReadList(player, level, undoStack, selection, list, listWriter, clipboard, clipboardWriter) {
    companion object : SplicingTableDataConverter<ReadListRangeToClipboard> {
        override fun test(view: SplicingTableClientView, selection: Selection?) =
            selection is Selection.Range && view.run { isListReadable && isClipboardWritable }

        override fun convert(data: SplicingTableData) = data.run {
            ReadListRangeToClipboard(
                player!!,
                level,
                undoStack,
                selection as Selection.Range,
                list!!,
                listWriter,
                clipboard,
                clipboardWriter!!,
            )
        }
    }
}

// simulate clearing the item, because that should be the most likely to succeed if the item is writable
// writing any other value might give a false negative for specialized items like scrolls
// we check if the write actually succeeded when running actions, so it's ok to be lax here
private fun ADIotaHolder.takeIfWritable() = takeIf { writeIota(null, true) }
