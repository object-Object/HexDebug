package gay.`object`.hexdebug.splicing

import at.petrak.hexcasting.api.casting.iota.Iota
import gay.`object`.hexdebug.utils.Option

data class UndoStack(
    val stack: MutableList<Entry> = mutableListOf(),
    var index: Int = -1,
) {
    val size get() = stack.size

    fun undo() = moveTo(index - 1)

    fun redo() = moveTo(index + 1)

    private fun moveTo(newIndex: Int) = stack.getOrNull(newIndex)?.also { index = newIndex }

    fun push(entry: Entry) {
        if (index < stack.lastIndex) {
            stack.subList(index + 1, stack.size).clear()
        }
        stack.add(entry)
        index += 1
    }

    data class Entry(
        val list: Option<List<Iota>>,
        val clipboard: Option<Iota?>,
        val selection: Option<Selection?>,
    ) {
        fun applyTo(data: SplicingTableData, defaultSelection: Selection?): Selection? = data.let {
            list.ifPresent(data::writeList)
            clipboard.ifPresent(data::writeClipboard)
            selection.getOrElse(defaultSelection)
        }
    }
}
