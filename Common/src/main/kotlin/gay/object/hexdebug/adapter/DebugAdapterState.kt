package gay.`object`.hexdebug.adapter

import gay.`object`.hexdebug.debugger.CastArgs
import gay.`object`.hexdebug.debugger.HexDebugger
import org.eclipse.lsp4j.debug.InitializeRequestArguments

sealed interface DebugAdapterState {
    var isConnected: Boolean
    var initArgs: InitializeRequestArguments
    var launchArgs: LaunchArgs
    val restartArgs: MutableMap<Int, CastArgs>

    data class NotDebugging(
        override var isConnected: Boolean = false,
        override var initArgs: InitializeRequestArguments = defaultInitArgs(),
        override var launchArgs: LaunchArgs = LaunchArgs(),
        override val restartArgs: MutableMap<Int, CastArgs> = mutableMapOf(),
    ) : DebugAdapterState {
        constructor(state: DebugAdapterState) : this(state.isConnected, state.initArgs, state.launchArgs, state.restartArgs)
    }

    data class Debugging(
        override var isConnected: Boolean,
        override var initArgs: InitializeRequestArguments,
        override var launchArgs: LaunchArgs,
        override val restartArgs: MutableMap<Int, CastArgs>,
        val debuggers: MutableMap<Int, HexDebugger>,
    ) : DebugAdapterState {
        constructor(state: DebugAdapterState, castArgs: CastArgs, threadId: Int = 0) : this(
            state.isConnected,
            state.initArgs,
            state.launchArgs,
            mutableMapOf(threadId to castArgs),
            mutableMapOf(threadId to HexDebugger(threadId, state.initArgs, state.launchArgs, castArgs)),
        )
    }
}

fun defaultInitArgs() = InitializeRequestArguments().apply {
    linesStartAt1 = true
    columnsStartAt1 = true
}
