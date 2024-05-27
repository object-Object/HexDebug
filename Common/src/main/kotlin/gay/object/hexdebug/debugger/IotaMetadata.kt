package gay.`object`.hexdebug.debugger

import org.eclipse.lsp4j.debug.InitializeRequestArguments
import org.eclipse.lsp4j.debug.OutputEventArguments
import org.eclipse.lsp4j.debug.Source
import org.eclipse.lsp4j.debug.StackFrame

data class IotaMetadata(
    val source: Source,
    val lineIndex: Int,
    val columnIndex: Int? = null,
) {
    var needsReload = false

    private var parenCount: Int? = null

    fun trySetParenCount(newParenCount: Int) {
        if (parenCount == null) {
            parenCount = newParenCount
            needsReload = true
        }
    }

    fun indent(width: Int) = " ".repeat(width * (parenCount ?: 0))

    fun line(initArgs: InitializeRequestArguments) = initArgs.indexToLine(lineIndex)

    fun column(initArgs: InitializeRequestArguments) = columnIndex?.let(initArgs::indexToColumn)

    fun toString(initArgs: InitializeRequestArguments) =
        listOfNotNull(source.name, line(initArgs), column(initArgs)).joinToString(":")
}

fun StackFrame.setSourceAndPosition(initArgs: InitializeRequestArguments, meta: IotaMetadata?) {
    if (meta == null) return
    source = meta.source
    line = meta.line(initArgs)
    meta.column(initArgs)?.also { column = it }
}

fun OutputEventArguments.setSourceAndPosition(initArgs: InitializeRequestArguments, meta: IotaMetadata?) {
    if (meta == null) return
    source = meta.source
    line = meta.line(initArgs)
    meta.column(initArgs)?.also { column = it }
}

fun InitializeRequestArguments.indexToLine(index: Int) = index + if (linesStartAt1) 1 else 0

fun InitializeRequestArguments.indexToColumn(index: Int) = index + if (columnsStartAt1) 1 else 0
