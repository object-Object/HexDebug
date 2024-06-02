package gay.`object`.hexdebug.splicing

enum class SplicingTableAction(val value: Value<*>) {
    // any data

    UNDO(Value(SplicingTableData) {
        undoStack.undo()?.applyTo(this) ?: selection
    }),

    REDO(Value(SplicingTableData) {
        undoStack.redo()?.applyTo(this) ?: selection
    }),

    // rw list range

    NUDGE_LEFT(Value(
        ReadWriteListRange,
        test = { it != null && it.start > 0 },
        validate = { selection.start > 0 },
    ) {
        list.add(selection.end, list.removeAt(selection.start - 1))
        writeList(list)
        selection.moveBy(-1)
    }),

    NUDGE_RIGHT(Value(
        ReadWriteListRange,
        test = { it is Selection.Range && list != null && it.end < list.lastIndex },
        validate = { selection.end < list.lastIndex },
    ) {
        list.add(selection.start, list.removeAt(selection.end + 1))
        writeList(list)
        selection.moveBy(1)
    }),

    DUPLICATE(Value(ReadWriteListRange) {
        selection
    }),

    DELETE(Value(ReadWriteListRange) {
        selection
    }),

    // rw list range, write clipboard

    CUT(Value(ReadWriteListRangeToClipboard) {
        selection
    }),

    COPY(Value(ReadWriteListRangeToClipboard) {
        selection
    }),

    // rw list, read clipboard

    PASTE(Value(ReadWriteListFromClipboard) {
        selection
    }),

    PASTE_SPLAT(Value(ReadWriteListFromClipboard) {
        selection
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
