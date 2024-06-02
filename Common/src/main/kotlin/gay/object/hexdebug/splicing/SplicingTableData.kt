package gay.`object`.hexdebug.splicing

import at.petrak.hexcasting.api.addldata.ADIotaHolder
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.mishaps.MishapOthersName
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

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
    open val player: ServerPlayer?,
    val level: ServerLevel,
    val undoStack: SplicingTableUndoStack,
    open val selection: Selection?,
    open val list: MutableList<Iota>?,
    open val listWriter: ADIotaHolder?,
    open val clipboard: Iota?,
    open val clipboardWriter: ADIotaHolder?,
) {
    constructor(
        player: ServerPlayer?,
        level: ServerLevel,
        undoStack: SplicingTableUndoStack,
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

    fun writeList(value: List<Iota>) = writeIota(listWriter, ListIota(value))

    fun writeClipboard(value: List<Iota>?) = writeClipboard(value?.let(::ListIota))

    fun writeClipboard(value: Iota?) = writeIota(clipboardWriter, value)

    fun writeIota(holder: ADIotaHolder?, value: Iota?): Boolean {
        if (holder == null || (value != null && IotaType.isTooLargeToSerialize(listOf(value)))) return false
        return holder.writeIota(value, false)
    }

    fun isClipboardTransferSafe(value: Iota) = isClipboardTransferSafe(listOf(value))

    fun isClipboardTransferSafe(value: List<Iota>) = null != MishapOthersName.getTrueNameFromArgs(value, player)

    companion object : SplicingTableDataConverter<SplicingTableData> {
        override fun test(view: SplicingTableClientView, selection: Selection?) = true
        override fun convert(data: SplicingTableData) = data
    }
}

open class ReadWriteList(
    player: ServerPlayer?,
    level: ServerLevel,
    undoStack: SplicingTableUndoStack,
    override val selection: Selection,
    override val list: MutableList<Iota>,
    override val listWriter: ADIotaHolder,
    clipboard: Iota?,
    clipboardWriter: ADIotaHolder?,
) : SplicingTableData(player, level, undoStack, selection, list, listWriter, clipboard, clipboardWriter) {
    companion object : SplicingTableDataConverter<ReadWriteList> {
        override fun test(view: SplicingTableClientView, selection: Selection?) =
            selection != null && view.run { isListReadable && isListWritable }

        override fun convert(data: SplicingTableData) = data.run {
            ReadWriteList(
                player,
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
    override val player: ServerPlayer,
    level: ServerLevel,
    undoStack: SplicingTableUndoStack,
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
    player: ServerPlayer?,
    level: ServerLevel,
    undoStack: SplicingTableUndoStack,
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
                player,
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
    undoStack: SplicingTableUndoStack,
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
    override val player: ServerPlayer,
    level: ServerLevel,
    undoStack: SplicingTableUndoStack,
    override val selection: Selection.Range,
    override val list: MutableList<Iota>,
    listWriter: ADIotaHolder?,
    clipboard: Iota?,
    override val clipboardWriter: ADIotaHolder,
) : SplicingTableData(player, level, undoStack, selection, list, listWriter, clipboard, clipboardWriter) {
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

private fun ADIotaHolder.takeIfWritable() = takeIf { writeIota(ListIota(listOf()), true) }
