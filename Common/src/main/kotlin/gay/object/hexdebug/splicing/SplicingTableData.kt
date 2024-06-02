package gay.`object`.hexdebug.splicing

import at.petrak.hexcasting.api.addldata.ADIotaHolder
import at.petrak.hexcasting.api.casting.SpellList
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.ListIota
import net.minecraft.server.level.ServerLevel

interface SplicingTableDataConverter<T : SplicingTableData> {
    fun test(view: SplicingTableClientView, selection: Selection?): Boolean

    fun convert(data: SplicingTableData): T

    fun convertOrNull(data: SplicingTableData): T? = try {
        convert(data)
    } catch (_: Throwable) {
        null
    }
}

open class SplicingTableData(
    val level: ServerLevel,
    val undoStack: SplicingTableUndoStack,
    open val selection: Selection?,
    open val list: SpellList?,
    open val listWriter: ADIotaHolder?,
    open val clipboard: Iota?,
    open val clipboardWriter: ADIotaHolder?,
) {
    constructor(
        level: ServerLevel,
        undoStack: SplicingTableUndoStack,
        selection: Selection?,
        listHolder: ADIotaHolder?,
        clipboardHolder: ADIotaHolder?,
    ) : this(
        level,
        undoStack,
        selection,
        list = listHolder?.let { it.readIota(level) as? ListIota }?.list,
        listWriter = listHolder?.takeIfWritable(),
        clipboard = clipboardHolder?.readIota(level),
        clipboardWriter = clipboardHolder?.takeIfWritable()
    )

    fun writeList(value: List<Iota>) = listWriter?.writeIota(ListIota(value), false) ?: false

    fun writeClipboard(value: List<Iota>) = writeClipboard(ListIota(value))

    fun writeClipboard(value: Iota) = clipboardWriter?.writeIota(value, false) ?: false

    companion object : SplicingTableDataConverter<SplicingTableData> {
        override fun test(view: SplicingTableClientView, selection: Selection?) = true
        override fun convert(data: SplicingTableData) = data
    }
}

open class ReadWriteList(
    level: ServerLevel,
    undoStack: SplicingTableUndoStack,
    selection: Selection,
    override val list: SpellList,
    override val listWriter: ADIotaHolder,
    clipboard: Iota?,
    clipboardWriter: ADIotaHolder?,
) : SplicingTableData(level, undoStack, selection, list, listWriter, clipboard, clipboardWriter) {
    companion object : SplicingTableDataConverter<ReadWriteList> {
        override fun test(view: SplicingTableClientView, selection: Selection?) =
            selection != null && view.run { isListReadable && isListWritable }

        override fun convert(data: SplicingTableData) = data.run {
            ReadWriteList(
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
    level: ServerLevel,
    undoStack: SplicingTableUndoStack,
    selection: Selection,
    list: SpellList,
    listWriter: ADIotaHolder,
    override val clipboard: Iota,
    clipboardWriter: ADIotaHolder?,
) : ReadWriteList(level, undoStack, selection, list, listWriter, clipboard, clipboardWriter) {
    companion object : SplicingTableDataConverter<ReadWriteListFromClipboard> {
        override fun test(view: SplicingTableClientView, selection: Selection?) =
            selection != null && view.run { isListReadable && isListWritable && isClipboardReadable }

        override fun convert(data: SplicingTableData) = data.run {
            ReadWriteListFromClipboard(
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
    level: ServerLevel,
    undoStack: SplicingTableUndoStack,
    override val selection: Selection.Range,
    list: SpellList,
    listWriter: ADIotaHolder,
    clipboard: Iota?,
    clipboardWriter: ADIotaHolder?,
) : ReadWriteList(level, undoStack, selection, list, listWriter, clipboard, clipboardWriter) {
    companion object : SplicingTableDataConverter<ReadWriteListRange> {
        override fun test(view: SplicingTableClientView, selection: Selection?) =
            selection is Selection.Range && view.run { isListReadable && isListWritable }

        override fun convert(data: SplicingTableData) = data.run {
            ReadWriteListRange(
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
    level: ServerLevel,
    undoStack: SplicingTableUndoStack,
    selection: Selection.Range,
    list: SpellList,
    listWriter: ADIotaHolder,
    clipboard: Iota?,
    override val clipboardWriter: ADIotaHolder,
) : ReadWriteListRange(level, undoStack, selection, list, listWriter, clipboard, clipboardWriter) {
    companion object : SplicingTableDataConverter<ReadWriteListRangeToClipboard> {
        override fun test(view: SplicingTableClientView, selection: Selection?) =
            selection is Selection.Range && view.run { isListReadable && isListWritable && isClipboardWritable }

        override fun convert(data: SplicingTableData) = data.run {
            ReadWriteListRangeToClipboard(
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

private fun ADIotaHolder.takeIfWritable() = takeIf { writeIota(ListIota(listOf()), true) }
