package gay.`object`.hexdebug.debugger

enum class ExceptionBreakpointType {
    UNCAUGHT_MISHAPS;

    val label get() = when (this) {
        UNCAUGHT_MISHAPS -> "Uncaught Mishaps"
    }

    val isDefault get() = when (this) {
        UNCAUGHT_MISHAPS -> true
    }
}
