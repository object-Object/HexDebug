package gay.`object`.hexdebug.splicing

import at.petrak.hexcasting.api.casting.iota.ListIota

enum class SplicingTableAction(val value: Value<*>) {
    // any data

    UNDO(Value(
        ReadList,
        test = { undoSize > 1 && undoIndex > 0 },
        validate = { undoStack.size > 1 && undoStack.index > 0 },
    ) {
        undoStack.undo()?.applyTo(this) ?: selection
    }),

    REDO(Value(
        ReadList,
        test = { undoSize > 1 && undoIndex < undoSize - 1 },
        validate = { undoStack.size > 1 && undoStack.index < undoStack.stack.lastIndex },
    ) {
        undoStack.redo()?.applyTo(this) ?: selection
    }),

    // rw list range

    NUDGE_LEFT(Value(
        ReadWriteListRange,
        test = { it != null && it.start > 0 },
        validate = { selection.start > 0 },
    ) {
        list.add(selection.end, list.removeAt(selection.start - 1))
        if (writeList(list)) {
            pushUndoState(selection.moveBy(-1))
        } else {
            selection
        }
    }),

    NUDGE_RIGHT(Value(
        ReadWriteListRange,
        test = { it is Selection.Range && list != null && it.end < list.lastIndex },
        validate = { selection.end < list.lastIndex },
    ) {
        list.add(selection.start, list.removeAt(selection.end + 1))
        if (writeList(list)) {
            pushUndoState(selection.moveBy(1))
        } else {
            selection
        }
    }),

    DUPLICATE(Value(ReadWriteListRange) {
        list.addAll(selection.end + 1, selection.subList(list))
        if (writeList(list)) {
            pushUndoState(selection.expandRight(selection.size))
        } else {
            selection
        }
    }),

    DELETE(Value(ReadWriteListRange) {
        selection.mutableSubList(list).clear()
        if (writeList(list)) {
            pushUndoState(null)
        } else {
            selection
        }
    }),

    // rw list range, write clipboard

    CUT(Value(ReadWriteListRangeToClipboard) {
        val sublist = selection.subList(list)
        selection.mutableSubList(list).clear()
        if (isClipboardTransferSafe(sublist) && writeClipboard(ListIota(sublist))) {
            pushUndoState(if (writeList(list)) null else selection)
        } else {
            selection
        }
    }),

    COPY(Value(ReadListRangeToClipboard) {
        val sublist = selection.subList(list)
        if (isClipboardTransferSafe(sublist) && writeClipboard(ListIota(sublist))) {
            pushUndoState(selection)
        }
        selection
    }),

    // rw list, read clipboard

    PASTE(Value(ReadWriteListFromClipboard) {
        selection.mutableSubList(list).apply {
            clear()
            add(clipboard)
        }
        if (isClipboardTransferSafe(clipboard) && writeList(list)) {
            pushUndoState(Selection.withSize(selection.start, 1))
        } else {
            selection
        }
    }),

    PASTE_SPLAT(Value(ReadWriteListFromClipboard) {
        val values = when (clipboard) {
            is ListIota -> clipboard.list.toList()
            else -> listOf(clipboard)
        }
        selection.mutableSubList(list).apply {
            clear()
            addAll(values)
        }
        if (isClipboardTransferSafe(clipboard) && writeList(list)) {
            pushUndoState(Selection.withSize(selection.start, values.size))
        } else {
            selection
        }
    });

    data class Value<T : SplicingTableData>(
        // runs on the client to check if the button should be enabled
        val test: SplicingTableClientView.(Selection?) -> Boolean,
        // runs on the server to ensure the required data is present
        val convert: SplicingTableData.() -> T?,
        // runs on the server to execute the action
        val run: T.() -> Selection?,
    ) {
        constructor(
            converter: SplicingTableDataConverter<T>,
            run: T.() -> Selection?,
        ) : this(converter::test, converter::convertOrNull, run)

        constructor(
            converter: SplicingTableDataConverter<T>,
            test: SplicingTableClientView.(Selection?) -> Boolean,
            validate: T.() -> Boolean,
            run: T.() -> Selection?,
        ) : this(
            test = { converter.test(this, it) && test(this, it) },
            convert = { converter.convertOrNull(this)?.takeIf(validate) },
            run = run,
        )

        fun convertAndRun(data: SplicingTableData): Selection? {
            val converted = convert(data) ?: return data.selection
            return run(converted)
        }
    }
}
