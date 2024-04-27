package gay.`object`.hexdebug.adapter

import gay.`object`.hexdebug.debugger.HexDebugger
import org.eclipse.lsp4j.debug.InitializeRequestArguments
import kotlin.reflect.KClass

class InvalidStateError(val state: String) : AssertionError("Unexpected state: $state") {
    constructor(state: DebugAdapterState) : this(state::class)

    constructor(stateType: KClass<out DebugAdapterState>) : this(stateType.simpleName ?: "Unknown")
}

inline fun <reified T : DebugAdapterState> DebugAdapterState.assert(): T {
    return this as? T ?: throw InvalidStateError(this)
}

sealed interface DebugAdapterState {
    val castArgs: CastArgs?

    data class NotDebugging(
        var initArgs: InitializeRequestArguments? = null,
        var launchArgs: LaunchArgs? = null,
        override var castArgs: CastArgs? = null,
    ) : DebugAdapterState

    data class Debugging(
        val initArgs: InitializeRequestArguments,
        val launchArgs: LaunchArgs,
        override val castArgs: CastArgs,
    ) : DebugAdapterState {
        val debugger = HexDebugger(initArgs, launchArgs, castArgs)
    }
}
