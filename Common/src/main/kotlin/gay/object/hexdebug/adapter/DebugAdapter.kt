package gay.`object`.hexdebug.adapter

import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.adapter.DebugAdapterState.*
import gay.`object`.hexdebug.adapter.proxy.DebugProxyServerLauncher
import gay.`object`.hexdebug.debugger.DebugStepResult
import gay.`object`.hexdebug.debugger.RequestStepType
import gay.`object`.hexdebug.items.ItemDebugger
import gay.`object`.hexdebug.networking.HexDebugNetworking
import gay.`object`.hexdebug.networking.MsgDebuggerStateS2C
import gay.`object`.hexdebug.utils.futureOf
import gay.`object`.hexdebug.utils.paginate
import gay.`object`.hexdebug.utils.toFuture
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import org.eclipse.lsp4j.debug.*
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer
import org.eclipse.lsp4j.jsonrpc.MessageConsumer
import java.util.concurrent.CompletableFuture

open class DebugAdapter(val player: ServerPlayer) : IDebugProtocolServer {
    private var state: DebugAdapterState = NotConnected
        set(value) {
            field = value
            setDebuggerState(value)
        }

    val isDebugging get() = state is Debugging

    val canStartDebugging get() = state is ReadyToDebug

    private val debugger get() = (state as? Debugging)?.debugger

    open val launcher: IHexDebugLauncher by lazy {
        DebugProxyServerLauncher.createLauncher(this, ::messageWrapper)
    }

    private val remoteProxy: IDebugProtocolClient get() = launcher.remoteProxy

    private fun setDebuggerState(state: DebugAdapterState) = setDebuggerState(
        when (state) {
            is Debugging -> ItemDebugger.DebugState.DEBUGGING
            else -> ItemDebugger.DebugState.NOT_DEBUGGING
        }
    )

    protected open fun setDebuggerState(debuggerState: ItemDebugger.DebugState) {
        HexDebugNetworking.sendToPlayer(player, MsgDebuggerStateS2C(debuggerState))
    }

    fun startDebugging(args: CastArgs): Boolean {
        val state = state as? NotDebugging ?: return false
        this.state = Debugging(state, args)
        remoteProxy.initialized()
        return true
    }

    fun disconnectClient() {
        if (state !is NotConnected) {
            remoteProxy.terminated(TerminatedEventArguments())
            state = NotConnected
        }
    }

    fun print(
        value: String,
        category: String = OutputEventArgumentsCategory.STDOUT,
        withSource: Boolean = true,
    ) {
        remoteProxy.output(OutputEventArguments().also {
            it.category = category
            it.output = value
            if (withSource) {
                debugger?.lastEvaluatedMetadata?.also { meta ->
                    it.source = meta.source
                    it.line = meta.line
                    if (meta.column != null) it.column = meta.column
                }
            }
        })
    }

    private fun messageWrapper(consumer: MessageConsumer) = MessageConsumer { message ->
        HexDebug.LOGGER.debug(message)
        consumer.consume(message)
    }

    private fun handleDebuggerStep(result: DebugStepResult?) {
        if (result == null) {
            terminate(null)
            return
        }

        for ((source, reason) in result.loadedSources) {
            remoteProxy.loadedSource(LoadedSourceEventArguments().also {
                it.source = source
                it.reason = reason
            })
        }

        sendStoppedEvent(result.reason)
    }

    private fun sendStoppedEvent(reason: String) {
        remoteProxy.stopped(StoppedEventArguments().also {
            it.threadId = 0
            it.reason = reason
        })
    }

    // initialization

    override fun initialize(args: InitializeRequestArguments): CompletableFuture<Capabilities> {
        state = Initialized(args)
        return Capabilities().apply {
            supportsConfigurationDoneRequest = true
            supportsLoadedSourcesRequest = true
            supportsTerminateRequest = true
            supportsRestartRequest = true
        }.toFuture()
    }

    override fun attach(args: MutableMap<String, Any>): CompletableFuture<Void> {
        val initArgs = state.initArgs ?: return futureOf()
        state = NotDebugging(initArgs, LaunchArgs(args))
        player.displayClientMessage(Component.translatable("text.hexdebug.connected"), true)
        return futureOf()
    }

    // configuration

    override fun setBreakpoints(args: SetBreakpointsArguments): CompletableFuture<SetBreakpointsResponse> {
        return SetBreakpointsResponse().apply {
            breakpoints = debugger?.setBreakpoints(args.source.sourceReference, args.breakpoints)?.toTypedArray() ?: arrayOf()
        }.toFuture()
    }

    override fun setExceptionBreakpoints(args: SetExceptionBreakpointsArguments): CompletableFuture<SetExceptionBreakpointsResponse> {
        // tell the client we didn't enable any of their breakpoints
        val count = args.filters.size + (args.filterOptions?.size ?: 0) + (args.exceptionOptions?.size ?: 0)
        val breakpoints = Array(count) { Breakpoint().apply { isVerified = false } }

        return SetExceptionBreakpointsResponse().apply {
            this.breakpoints = breakpoints
        }.toFuture()
    }

    override fun configurationDone(args: ConfigurationDoneArguments?): CompletableFuture<Void> {
        handleDebuggerStep(debugger?.start())
        return futureOf()
    }

    // stepping

    override fun next(args: NextArguments?): CompletableFuture<Void> {
        handleDebuggerStep(debugger?.executeUntilStopped(RequestStepType.OVER))
        return futureOf()
    }

    override fun continue_(args: ContinueArguments?): CompletableFuture<ContinueResponse> {
        handleDebuggerStep(debugger?.executeUntilStopped())
        return futureOf()
    }

    override fun stepIn(args: StepInArguments?): CompletableFuture<Void> {
        handleDebuggerStep(debugger?.executeOnce())
        return futureOf()
    }

    override fun stepOut(args: StepOutArguments?): CompletableFuture<Void> {
        handleDebuggerStep(debugger?.executeUntilStopped(RequestStepType.OUT))
        return futureOf()
    }

    override fun pause(args: PauseArguments): CompletableFuture<Void> {
        // TODO: wisps/circles?
        return futureOf()
    }

    override fun restart(args: RestartArguments?): CompletableFuture<Void> {
        val state = state
        if (state is ReadyToDebug) {
            this.state = NotDebugging(state)
            state.restartArgs?.run(::startDebugging)
        }
        return futureOf()
    }

    override fun terminate(args: TerminateArguments?): CompletableFuture<Void> {
        val state = state
        if (state is ReadyToDebug) {
            this.state = NotDebugging(state)
            remoteProxy.exited(ExitedEventArguments().also { it.exitCode = 0 })
            if (state is Debugging) {
                for (source in state.debugger.getSources()) {
                    remoteProxy.loadedSource(LoadedSourceEventArguments().also {
                        it.source = source
                        it.reason = LoadedSourceEventArgumentsReason.REMOVED
                    })
                }
            }
            remoteProxy.invalidated(InvalidatedEventArguments())
        }
        return futureOf()
    }

    override fun disconnect(args: DisconnectArguments?): CompletableFuture<Void> {
        state = NotConnected
        return futureOf()
    }

    // runtime data

    override fun threads(): CompletableFuture<ThreadsResponse> {
        // always return the same dummy thread - we don't support multithreading
        return ThreadsResponse().apply {
            threads = arrayOf(Thread().apply {
                id = 0
                name = "Main Thread"
            })
        }.toFuture()
    }

    override fun scopes(args: ScopesArguments): CompletableFuture<ScopesResponse> {
        return ScopesResponse().apply {
            scopes = debugger?.getScopes(args.frameId)?.toTypedArray() ?: arrayOf()
        }.toFuture()
    }

    override fun variables(args: VariablesArguments): CompletableFuture<VariablesResponse> {
        return VariablesResponse().apply {
            variables = debugger?.getVariables(args.variablesReference)?.paginate(args.start, args.count) ?: arrayOf()
        }.toFuture()
    }

    override fun stackTrace(args: StackTraceArguments): CompletableFuture<StackTraceResponse> {
        return StackTraceResponse().apply {
            stackFrames = debugger?.getStackFrames()?.paginate(args.startFrame, args.levels) ?: arrayOf()
        }.toFuture()
    }

    override fun source(args: SourceArguments): CompletableFuture<SourceResponse> {
        return SourceResponse().apply {
            content = debugger?.getSourceContents(args.source.sourceReference) ?: ""
        }.toFuture()
    }

    override fun loadedSources(args: LoadedSourcesArguments?): CompletableFuture<LoadedSourcesResponse> {
        return LoadedSourcesResponse().apply {
            sources = debugger?.getSources()?.toTypedArray() ?: arrayOf()
        }.toFuture()
    }
}
