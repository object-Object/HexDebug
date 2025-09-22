package gay.`object`.hexdebug.splicing

import at.petrak.hexcasting.api.casting.iota.ListIota
import gay.`object`.hexdebug.utils.Option.Some

enum class SplicingTableAction(val value: Value<*>) {
    // any data

    VIEW_LEFT(Value(
        ReadList,
        consumesMedia = false,
        test = { _, viewStartIndex -> viewStartIndex > 0 },
        validate = { viewStartIndex > 0 },
    ) {
        viewStartIndex -= 1
    }),

    VIEW_LEFT_PAGE(Value(
        ReadList,
        consumesMedia = false,
        test = { _, viewStartIndex -> viewStartIndex > 0 },
        validate = { viewStartIndex > 0 },
    ) {
        viewStartIndex -= IOTA_BUTTONS
    }),

    VIEW_LEFT_FULL(Value(
        ReadList,
        consumesMedia = false,
        test = { _, viewStartIndex -> viewStartIndex > 0 },
        validate = { viewStartIndex > 0 },
    ) {
        viewStartIndex = 0
    }),

    VIEW_RIGHT(Value(
        ReadList,
        consumesMedia = false,
        test = { _, viewStartIndex -> viewStartIndex + VIEW_END_INDEX_OFFSET < lastIndex },
        validate = { viewEndIndex < list.lastIndex },
    ) {
        viewEndIndex += 1
    }),

    VIEW_RIGHT_PAGE(Value(
        ReadList,
        consumesMedia = false,
        test = { _, viewStartIndex -> viewStartIndex + VIEW_END_INDEX_OFFSET < lastIndex },
        validate = { viewEndIndex < list.lastIndex },
    ) {
        viewEndIndex += IOTA_BUTTONS
    }),

    VIEW_RIGHT_FULL(Value(
        ReadList,
        consumesMedia = false,
        test = { _, viewStartIndex -> viewStartIndex + VIEW_END_INDEX_OFFSET < lastIndex },
        validate = { viewEndIndex < list.lastIndex },
    ) {
        viewEndIndex = list.lastIndex
    }),

    SELECT_NONE(Value(
        ReadList,
        consumesMedia = false,
        test = { selection, _ -> selection != null },
        validate = { selection != null },
    ) {
        selection = null
    }),

    SELECT_ALL(Value(
        ReadList,
        consumesMedia = false,
        test = { selection, _ -> selection != Selection.range(0, lastIndex) },
        validate = { selection != Selection.range(0, list.lastIndex) },
    ) {
        selection = Selection.range(0, list.lastIndex)
    }),

    UNDO(Value(
        ReadList,
        consumesMedia = true,
        test = { _, _ -> undoSize > 1 && undoIndex > 0 },
        validate = { undoStack.size > 1 && undoStack.index > 0 },
    ) {
        undoStack.undo()?.applyTo(this)
    }),

    REDO(Value(
        ReadList,
        consumesMedia = true,
        test = { _, _ -> undoSize > 1 && undoIndex < undoSize - 1 },
        validate = { undoStack.size > 1 && undoStack.index < undoStack.stack.lastIndex },
    ) {
        undoStack.redo()?.applyTo(this)
    }),

    // rw list range

    NUDGE_LEFT(Value(
        ReadWriteListRange,
        consumesMedia = true,
        test = { selection, _ -> selection != null && selection.start > 0 },
        validate = { typedSelection.start > 0 },
    ) {
        list.add(typedSelection.end, list.removeAt(typedSelection.start - 1))
        if (writeList(list)) {
            selection = typedSelection.moveBy(-1)?.also {
                makeIotaVisible(it.start)
            }
            pushUndoState(
                list = Some(list),
                selection = Some(selection),
            )
        }
    }),

    NUDGE_RIGHT(Value(
        ReadWriteListRange,
        consumesMedia = true,
        test = { selection, _ -> selection is Selection.Range && list != null && selection.end < list.lastIndex },
        validate = { typedSelection.end < list.lastIndex },
    ) {
        list.add(typedSelection.start, list.removeAt(typedSelection.end + 1))
        if (writeList(list)) {
            selection = typedSelection.moveBy(1)?.also {
                makeIotaVisible(it.lastIndex)
            }
            pushUndoState(
                list = Some(list),
                selection = Some(selection),
            )
        }
    }),

    DUPLICATE(Value(ReadWriteListRange, consumesMedia = true) {
        list.addAll(typedSelection.end + 1, typedSelection.subList(list))
        if (writeList(list)) {
            selection = Selection.withSize(typedSelection.end + 1, typedSelection.size)?.also {
                makeIotaVisible(it.end)
            }
            pushUndoState(
                list = Some(list),
                selection = Some(selection),
            )
        }
    }),

    DELETE(Value(ReadWriteListRange, consumesMedia = true) {
        typedSelection.mutableSubList(list).clear()
        if (writeList(list)) {
            selection = Selection.edge(typedSelection.start)?.also {
                makeEdgeVisible(it.index)
            }
            pushUndoState(
                list = Some(list),
                selection = Some(selection),
            )
        }
    }),

    // rw list range, write clipboard

    CUT(Value(ReadWriteListRangeToClipboard, consumesMedia = true) {
        val iota = typedSelection.subList(list).let { if ((it.size) == 1) it.first() else ListIota(it) }
        typedSelection.mutableSubList(list).clear()
        if (isClipboardTransferSafe(iota) && writeClipboard(iota)) {
            if (writeList(list)) {
                selection = Selection.edge(typedSelection.start)?.also {
                    makeEdgeVisible(it.index)
                }
                pushUndoState(
                    list = Some(list),
                    clipboard = Some(iota),
                    selection = Some(selection),
                )
            } else {
                pushUndoState(
                    clipboard = Some(iota),
                )
            }
        }
    }),

    COPY(Value(ReadListRangeToClipboard, consumesMedia = true) {
        val iota = typedSelection.subList(list).let { if ((it.size) == 1) it.first() else ListIota(it) }
        if (isClipboardTransferSafe(iota) && writeClipboard(iota)) {
            pushUndoState(
                clipboard = Some(iota),
            )
        }
    }),

    // rw list, read clipboard

    PASTE_VERBATIM(Value(ReadWriteListFromClipboard, consumesMedia = true) {
        typedSelection.mutableSubList(list).apply {
            clear()
            add(clipboard)
        }
        if (isClipboardTransferSafe(clipboard) && writeList(list)) {
            selection = Selection.edge(typedSelection.start + 1)?.also {
                makeEdgeVisible(it.index)
            }
            pushUndoState(
                list = Some(list),
                selection = Some(selection),
            )
        }
    }),

    PASTE_SPLAT(Value(ReadWriteListFromClipboard, consumesMedia = true) {
        val values = when (clipboard) {
            is ListIota -> clipboard.list.toList()
            else -> listOf(clipboard)
        }
        typedSelection.mutableSubList(list).apply {
            clear()
            addAll(values)
        }
        if (isClipboardTransferSafe(clipboard) && writeList(list)) {
            selection = Selection.edge(typedSelection.start + values.size)?.also {
                makeEdgeVisible(it.index)
            }
            pushUndoState(
                list = Some(list),
                selection = Some(selection),
            )
        } else {
            selection
        }
    });

    data class Value<T : SplicingTableData>(
        val consumesMedia: Boolean,
        // runs on the client to check if the button should be enabled
        // arguments: selection, viewStartIndex
        val test: SplicingTableClientView.(Selection?, Int) -> Boolean,
        // runs on the server to ensure the required data is present
        val convert: SplicingTableData.() -> T?,
        // runs on the server to execute the action
        val run: T.() -> Unit,
    ) {
        constructor(
            converter: SplicingTableDataConverter<T>,
            consumesMedia: Boolean,
            run: T.() -> Unit,
        ) : this(consumesMedia, converter::test, converter::convertOrNull, run)

        constructor(
            converter: SplicingTableDataConverter<T>,
            consumesMedia: Boolean,
            test: SplicingTableClientView.(Selection?, Int) -> Boolean,
            validate: T.() -> Boolean,
            run: T.() -> Unit,
        ) : this(
            consumesMedia,
            test = { selection, viewStartIndex ->
                converter.test(this, selection, viewStartIndex) && test(this, selection, viewStartIndex)
            },
            convert = { converter.convertOrNull(this)?.takeIf(validate) },
            run = run,
        )
    }
}
