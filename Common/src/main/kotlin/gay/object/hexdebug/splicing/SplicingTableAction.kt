package gay.`object`.hexdebug.splicing

enum class SplicingTableAction(val value: Value<*>) {
    // any data
    UNDO(Value(SplicingTableData) {
        selection
    }),
    REDO(Value(SplicingTableData) {
        selection
    }),
    // rw list range
    NUDGE_LEFT(Value(ReadWriteListRange) {
        selection
    }),
    NUDGE_RIGHT(Value(ReadWriteListRange) {
        selection
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
        val test: SplicingTableClientView.(Selection?) -> Boolean,
        val convert: SplicingTableData.() -> T?,
        val run: T.() -> Selection?,
    ) {
        constructor(
            converter: SplicingTableDataConverter<T>,
            run: T.() -> Selection?,
        ) : this(converter::test, converter::convertOrNull, run)
    }
}
