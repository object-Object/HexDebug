package ca.objectobject.hexdebug.debugger

import org.eclipse.lsp4j.debug.Source

data class IotaMetadata(
    val source: Source,
    val line: Int,
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
}
