package gay.`object`.hexdebug.debugger

import org.eclipse.lsp4j.debug.StoppedEventArgumentsReason

enum class DebuggerState(val canPause: Boolean = false, val canResume: Boolean = false) {
    RUNNING(canPause = true),
    PAUSING(canPause = true),
    PAUSED(canResume = true),
    CAUGHT_MISHAP(canResume = true),
    TERMINATED,
}

enum class StopReason(val value: String?, val stopImmediately: Boolean) {
    STEP(StoppedEventArgumentsReason.STEP, false),
    PAUSE(StoppedEventArgumentsReason.PAUSE, true),
    BREAKPOINT(StoppedEventArgumentsReason.BREAKPOINT, true),
    EXCEPTION(StoppedEventArgumentsReason.EXCEPTION, true),
    STARTED(StoppedEventArgumentsReason.ENTRY, true),
    TERMINATED(null, true),
}

enum class SourceBreakpointMode(val label: String, val description: String) {
    EVALUATED("Evaluated", "Stop if this iota would be evaluated. (default)"),
    ESCAPED("Escaped", "Stop if this iota would be escaped."),
    ALL("All", "Always stop at this iota."),
}

enum class ExceptionBreakpointType(val label: String, val isDefault: Boolean) {
    UNCAUGHT_MISHAPS("Uncaught Mishaps", true),
}
