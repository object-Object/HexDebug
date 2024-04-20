package ca.objectobject.hexdebug.debugger

import org.eclipse.lsp4j.debug.Source

data class IotaMetadata(
    val source: Source,
    val line: Int,
    var parenCount: Int = 0,
) {
    fun indent(width: Int) = " ".repeat(width * parenCount)
}
