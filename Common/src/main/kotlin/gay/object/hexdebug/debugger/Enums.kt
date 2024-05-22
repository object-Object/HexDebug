package gay.`object`.hexdebug.debugger

enum class DebugStepType {
    IN,
    OUT,
    JUMP,
    ESCAPE,
}

enum class RequestStepType {
    OVER,
    OUT,
}

enum class SourceBreakpointMode {
    EVALUATED,
    ESCAPED,
    ALL;

    val label get() = when (this) {
        EVALUATED -> "Evaluated"
        ESCAPED -> "Escaped"
        ALL -> "All"
    }

    val description get() = when (this) {
        EVALUATED -> "Stop if this iota would be evaluated. (default)"
        ESCAPED -> "Stop if this iota would be escaped."
        ALL -> "Always stop at this iota."
    }
}

enum class ExceptionBreakpointType {
    UNCAUGHT_MISHAPS;

    val label get() = when (this) {
        UNCAUGHT_MISHAPS -> "Uncaught Mishaps"
    }

    val isDefault get() = when (this) {
        UNCAUGHT_MISHAPS -> true
    }
}
