package gay.`object`.hexdebug.debugger

enum class DebuggerState(val canPause: Boolean = false, val canResume: Boolean = false) {
    RUNNING(canPause = true),
    PAUSING(canPause = true),
    PAUSED(canResume = true),
    CAUGHT_MISHAP(canResume = true),
    TERMINATED,
}

enum class SourceBreakpointMode(val label: String, val description: String) {
    EVALUATED("Evaluated", "Stop if this iota would be evaluated. (default)"),
    ESCAPED("Escaped", "Stop if this iota would be escaped."),
    ALL("All", "Always stop at this iota."),
}

enum class ExceptionBreakpointType(val label: String, val isDefault: Boolean) {
    UNCAUGHT_MISHAPS("Uncaught Mishaps", true),
}
