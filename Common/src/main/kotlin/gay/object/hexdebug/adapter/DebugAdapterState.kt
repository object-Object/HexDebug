package gay.`object`.hexdebug.adapter

import gay.`object`.hexdebug.debugger.HexDebugger
import org.eclipse.lsp4j.debug.InitializeRequestArguments

sealed interface DebugAdapterState {
    val initArgs: InitializeRequestArguments? get() = null

    data object NotConnected : DebugAdapterState

    data class Initialized(override val initArgs: InitializeRequestArguments) : DebugAdapterState

    sealed interface ReadyToDebug : DebugAdapterState {
        override val initArgs: InitializeRequestArguments
        val launchArgs: LaunchArgs
        val restartArgs: CastArgs? get() = null
    }

    data class NotDebugging(
        override val initArgs: InitializeRequestArguments,
        override val launchArgs: LaunchArgs,
        override val restartArgs: CastArgs? = null,
    ) : ReadyToDebug {
        constructor(state: ReadyToDebug) : this(state.initArgs, state.launchArgs, state.restartArgs)
    }

    data class Debugging(
        override val initArgs: InitializeRequestArguments,
        override val launchArgs: LaunchArgs,
        val castArgs: CastArgs,
    ) : ReadyToDebug {
        constructor(state: ReadyToDebug, castArgs: CastArgs) : this(state.initArgs, state.launchArgs, castArgs)

        val debugger = HexDebugger(initArgs, launchArgs, castArgs)

        override val restartArgs get() = castArgs
    }
}
