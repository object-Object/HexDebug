package gay.`object`.hexdebug.debugger

enum class StopReason(val value: String) {
    STEP("step"),
    BREAKPOINT("breakpoint"),
    EXCEPTION("exception"),
    ENTRY("entry"),
    TERMINATED("terminated"),
}

enum class DebugStepType {
    IN,
    OUT,
    JUMP,
    ESCAPE,
}

enum class SourceBreakpointMode(val label: String, val description: String) {
    EVALUATED("Evaluated", "Stop if this iota would be evaluated. (default)"),
    ESCAPED("Escaped", "Stop if this iota would be escaped."),
    ALL("All", "Always stop at this iota."),
}

enum class ExceptionBreakpointType(val label: String, val isDefault: Boolean) {
    UNCAUGHT_MISHAPS("Uncaught Mishaps", true),
}
