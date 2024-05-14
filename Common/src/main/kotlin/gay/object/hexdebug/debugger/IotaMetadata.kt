package gay.`object`.hexdebug.debugger

import org.eclipse.lsp4j.debug.Source
import org.eclipse.lsp4j.debug.StackFrame

data class IotaMetadata(
    val source: Source,
    val line: Int,
    val column: Int? = null,
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

    override fun toString() = "${source.name}:$line" + if (column != null) ":$column" else ""
}

fun StackFrame.setSourceAndPosition(meta: IotaMetadata?) {
    if (meta == null) return
    source = meta.source
    line = meta.line
    meta.column?.let { column = it }
}
