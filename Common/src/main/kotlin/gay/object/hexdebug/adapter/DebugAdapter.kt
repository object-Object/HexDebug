package gay.`object`.hexdebug.adapter

import at.petrak.hexcasting.api.casting.SpellList
import at.petrak.hexcasting.api.casting.eval.ExecutionClientView
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.common.msgs.MsgNewSpellPatternS2C
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.adapter.DebugAdapterState.*
import gay.`object`.hexdebug.adapter.proxy.DebugProxyServerLauncher
import gay.`object`.hexdebug.debugger.*
import gay.`object`.hexdebug.items.ItemDebugger
import gay.`object`.hexdebug.items.ItemEvaluator
import gay.`object`.hexdebug.networking.HexDebugNetworking
import gay.`object`.hexdebug.networking.MsgDebuggerStateS2C
import gay.`object`.hexdebug.networking.MsgEvaluatorStateS2C
import gay.`object`.hexdebug.utils.futureOf
import gay.`object`.hexdebug.utils.paginate
import gay.`object`.hexdebug.utils.toFuture
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import org.eclipse.lsp4j.debug.*
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer
import org.eclipse.lsp4j.jsonrpc.MessageConsumer
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode
import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

open class DebugAdapter(val player: ServerPlayer) : IDebugProtocolServer {
    private var state: DebugAdapterState = NotConnected
        set(value) {
            field = value
            setDebuggerState(value)
        }

    val isDebugging get() = state is Debugging

    val canStartDebugging get() = state is ReadyToDebug

    // this is protected so outside classes trying to affect the active debug session must go through the adapter
    // which ensures the debug client is kept up-to-date
    protected val debugger get() = (state as? Debugging)?.debugger

    val evaluatorUIPatterns get() = debugger?.evaluatorUIPatterns

    fun generateDescs() = debugger?.generateDescs()

    open val launcher: IHexDebugLauncher by lazy {
        DebugProxyServerLauncher.createLauncher(this, ::messageWrapper, ::exceptionHandler)
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

        // close the debugger grid if we stopped debugging
        if (debuggerState == ItemDebugger.DebugState.NOT_DEBUGGING) {
            val info = ExecutionClientView(true, ResolvedPatternType.EVALUATED, listOf(), null)
            IXplatAbstractions.INSTANCE.sendPacketToPlayer(player, MsgNewSpellPatternS2C(info, -1))
        }
    }

    protected open fun setEvaluatorState(evalState: ItemEvaluator.EvalState) {
        HexDebugNetworking.sendToPlayer(player, MsgEvaluatorStateS2C(evalState))
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

    fun evaluate(pattern: HexPattern) = evaluate(PatternIota(pattern))

    fun evaluate(iota: Iota) = evaluate(SpellList.LList(listOf(iota)))

    fun evaluate(list: SpellList) = debugger?.let {
        val result = it.evaluate(list)
        if (result?.startedEvaluating == true) {
            setEvaluatorState(ItemEvaluator.EvalState.MODIFIED)
        }
        handleDebuggerStep(result)
    }

    fun resetEvaluator() {
        setEvaluatorState(ItemEvaluator.EvalState.DEFAULT)
        debugger?.also {
            it.resetEvaluator()
            sendStoppedEvent("step")
        }
    }

    private fun messageWrapper(consumer: MessageConsumer) = MessageConsumer { message ->
        HexDebug.LOGGER.debug(message)
        consumer.consume(message)
    }

    private fun exceptionHandler(e: Throwable): ResponseError {
        HexDebug.LOGGER.error(e.stackTraceToString())
        return (e as? ResponseErrorException)?.responseError
            ?: e.takeIf { it is CompletionException || it is InvocationTargetException }
                ?.run { cause as? ResponseErrorException }
                ?.responseError
            ?: ResponseError(ResponseErrorCode.InternalError, e.toString(), e.stackTraceToString())
    }

    private fun handleDebuggerStep(result: DebugStepResult?): ExecutionClientView? {
        val view = debugger?.getClientView()?.also {
            IXplatAbstractions.INSTANCE.sendPacketToPlayer(player, MsgNewSpellPatternS2C(it, -1))
        }

        if (result == null) {
            terminate(null)
            return view
        }

        for ((source, reason) in result.loadedSources) {
            remoteProxy.loadedSource(LoadedSourceEventArguments().also {
                it.source = source
                it.reason = reason
            })
        }

        sendStoppedEvent(result.reason)
        return view
    }

    private fun sendStoppedEvent(reason: String) {
        remoteProxy.stopped(StoppedEventArguments().also {
            it.threadId = 0
            it.reason = reason
        })
    }

    private fun invalidateBreakpoints(n: Int) = Array(n) {
        Breakpoint().apply {
            isVerified = false
            message = "Invalid"
            reason = BreakpointNotVerifiedReason.FAILED
        }
    }

    // initialization

    override fun initialize(args: InitializeRequestArguments): CompletableFuture<Capabilities> {
        state = Initialized(args)
        return Capabilities().apply {
            supportsConfigurationDoneRequest = true
            supportsLoadedSourcesRequest = true
            supportsTerminateRequest = true
            supportsRestartRequest = true
            exceptionBreakpointFilters = ExceptionBreakpointType.entries.map {
                ExceptionBreakpointsFilter().apply {
                    filter = it.name
                    label = it.label
                    default_ = it.isDefault
                }
            }.toTypedArray()
            breakpointModes = SourceBreakpointMode.entries.map {
                BreakpointMode().apply {
                    mode = it.name
                    label = it.label
                    description = it.description
                    appliesTo = arrayOf(BreakpointModeApplicability.SOURCE)
                }
            }.toTypedArray()
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
            // source prefixes generally invalidate when the server restarts
            // so remove all breakpoints if we haven't seen this player before
            breakpoints = if (player.uuid in knownPlayers) {
                debugger?.setBreakpoints(args.source.sourceReference, args.breakpoints)?.toTypedArray() ?: arrayOf()
            } else {
                invalidateBreakpoints(args.breakpoints.size)
            }
        }.toFuture()
    }

    override fun setExceptionBreakpoints(args: SetExceptionBreakpointsArguments): CompletableFuture<SetExceptionBreakpointsResponse> {
        return SetExceptionBreakpointsResponse().apply {
            breakpoints = debugger?.setExceptionBreakpoints(args.filters)?.toTypedArray() ?: arrayOf()
        }.toFuture()
    }

    override fun configurationDone(args: ConfigurationDoneArguments?): CompletableFuture<Void> {
        knownPlayers.add(player.uuid)
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

    companion object {
        // set of players that have connected since the server started
        // this is used to invalidate old client-side debugger data if necessary
        private val knownPlayers = mutableSetOf<UUID>()
    }
}
