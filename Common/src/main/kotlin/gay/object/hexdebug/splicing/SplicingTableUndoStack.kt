package gay.`object`.hexdebug.splicing

import at.petrak.hexcasting.api.casting.iota.Iota

data class SplicingTableUndoStack(
    val stack: MutableList<Entry> = mutableListOf(),
    var index: Int = -1,
) {
    val size get() = stack.size

    fun undo() = moveTo(index - 1)

    fun redo() = moveTo(index + 1)

    private fun moveTo(newIndex: Int) = stack.getOrNull(newIndex)?.also { index = newIndex }

    fun push(list: List<Iota>, clipboard: Iota?, selection: Selection?) = push(Entry(list, clipboard, selection))

    private fun push(entry: Entry) {
        if (index < stack.lastIndex) {
            stack.subList(index + 1, stack.size).clear()
        }
        stack.add(entry)
        index += 1
    }

    data class Entry(
        val list: List<Iota>,
        val clipboard: Iota?,
        val selection: Selection?,
    ) {
        fun applyTo(data: SplicingTableData) = data.let {
            it.writeList(list)
            it.writeClipboard(clipboard)
            selection
        }
    }
}
