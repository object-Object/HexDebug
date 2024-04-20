package ca.objectobject.hexdebug.debugger

data class DebugStepResult(
    val reason: String,
    val type: DebugStepType? = null,
) {
    @Suppress("UNUSED_PARAMETER")
    operator fun plus(lastResult: DebugStepResult) = this
}

enum class DebugStepType {
    IN,
    OUT,
    JUMP,
    ESCAPE,
}
