package gay.`object`.hexdebug.adapter

import gay.`object`.hexdebug.debugger.CastArgs
import gay.`object`.hexdebug.debugger.HexDebugger
import org.eclipse.lsp4j.debug.InitializeRequestArguments

sealed interface DebugAdapterState {
    var isConnected: Boolean
    var initArgs: InitializeRequestArguments
    var launchArgs: LaunchArgs
    val restartArgs: CastArgs?

    data class NotDebugging(
        override var isConnected: Boolean = false,
        override var initArgs: InitializeRequestArguments = InitializeRequestArguments(),
        override var launchArgs: LaunchArgs = LaunchArgs(),
        override val restartArgs: CastArgs? = null,
    ) : DebugAdapterState {
        constructor(state: DebugAdapterState) : this(state.isConnected, state.initArgs, state.launchArgs, state.restartArgs)
    }

    data class Debugging(
        override var isConnected: Boolean,
        override val restartArgs: CastArgs,
        val debugger: HexDebugger,
    ) : DebugAdapterState {
        constructor(state: DebugAdapterState, castArgs: CastArgs) : this(
            state.isConnected,
            castArgs,
            HexDebugger(state.initArgs, state.launchArgs, castArgs),
        )

        override var initArgs by debugger::initArgs
        override var launchArgs by debugger::launchArgs
    }
}
