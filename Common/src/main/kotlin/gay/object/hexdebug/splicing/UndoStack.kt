package gay.`object`.hexdebug.splicing

import at.petrak.hexcasting.api.spell.iota.Iota
import gay.`object`.hexdebug.config.HexDebugConfig
import gay.`object`.hexdebug.utils.Option
import gay.`object`.hexdebug.utils.Option.Some

data class UndoStack(
    val stack: MutableList<Entry> = mutableListOf(),
    var index: Int = -1,
) {
    val size get() = stack.size

    private val maxSize get() = HexDebugConfig.server.maxUndoStackSize

    fun undo() = moveTo(index - 1)

    fun redo() = moveTo(index + 1)

    private fun moveTo(newIndex: Int) = stack.getOrNull(newIndex)?.also { index = newIndex }

    fun push(entry: Entry) {
        // drop entries from the top of the stack above the current index
        if (index < stack.lastIndex) {
            stack.subList(index + 1, stack.size).clear()
        }

        stack.add(entry)

        // drop entries from the bottom of the stack to enforce the size limit
        if (maxSize > 0 && stack.size > maxSize) {
            stack.subList(0, stack.size - maxSize).clear()
        }

        index = stack.lastIndex
    }

    fun clear() {
        stack.clear()
        index = -1
    }

    data class Entry(
        val list: Option<List<Iota>>,
        val clipboard: Option<Iota?>,
        val selection: Option<Selection?>,
    ) {
        val isNotEmpty = list is Some || clipboard is Some || selection is Some

        fun applyTo(data: SplicingTableData, defaultSelection: Selection?): Selection? = data.let {
            list.ifPresent(data::writeList)
            clipboard.ifPresent(data::writeClipboard)
            selection.getOrElse(defaultSelection)
        }
    }
}
