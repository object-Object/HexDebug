package gay.`object`.hexdebug.adapter

import at.petrak.hexcasting.api.casting.SpellList
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.ExecutionClientView
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexPattern
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.adapter.DebugAdapterState.Debugging
import gay.`object`.hexdebug.adapter.DebugAdapterState.NotDebugging
import gay.`object`.hexdebug.adapter.proxy.DebugProxyServerLauncher
import gay.`object`.hexdebug.debugger.*
import gay.`object`.hexdebug.items.DebuggerItem
import gay.`object`.hexdebug.items.EvaluatorItem
import gay.`object`.hexdebug.networking.msg.MsgDebuggerStateS2C
import gay.`object`.hexdebug.networking.msg.MsgEvaluatorClientInfoS2C
import gay.`object`.hexdebug.networking.msg.MsgEvaluatorStateS2C
import gay.`object`.hexdebug.networking.msg.MsgPrintDebuggerStatusS2C
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
    private var state: DebugAdapterState = NotDebugging()
        set(value) {
            field = value
            setDebuggerState(value)
        }

    val isDebugging get() = state is Debugging

    val debugger get() = (state as? Debugging)?.debugger

    open val launcher: IHexDebugLauncher by lazy {
        DebugProxyServerLauncher.createLauncher(this, ::messageWrapper, ::exceptionHandler)
    }

    private val remoteProxy: IDebugProtocolClient get() = launcher.remoteProxy

    private fun setDebuggerState(state: DebugAdapterState) = setDebuggerState(
        when (state) {
            is Debugging -> DebuggerItem.DebugState.DEBUGGING
            else -> DebuggerItem.DebugState.NOT_DEBUGGING
        }
    )

    protected open fun setDebuggerState(debuggerState: DebuggerItem.DebugState) {
        MsgDebuggerStateS2C(debuggerState).sendToPlayer(player)

        // close the evaluator grid if we stopped debugging
        if (debuggerState == DebuggerItem.DebugState.NOT_DEBUGGING) {
            val info = ExecutionClientView(true, ResolvedPatternType.EVALUATED, listOf(), null)
            MsgEvaluatorClientInfoS2C(info).sendToPlayer(player)
        }
    }

    protected open fun setEvaluatorState(evalState: EvaluatorItem.EvalState) {
        MsgEvaluatorStateS2C(evalState).sendToPlayer(player)
    }

    protected open fun printDebuggerStatus(iota: String, index: Int) {
        MsgPrintDebuggerStatusS2C(
            iota = iota,
            index = index,
            line = state.initArgs.indexToLine(index),
            isConnected = state.isConnected,
        ).sendToPlayer(player)
    }

    fun startDebugging(args: CastArgs): Boolean {
        if (state is Debugging) return false
        val state = Debugging(state, args).also { state = it }
        handleDebuggerStep(state.debugger.start())
        return true
    }

    fun disconnectClient() {
        if (state.isConnected) {
            remoteProxy.terminated(TerminatedEventArguments())
            state.isConnected = false
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
                it.setSourceAndPosition(state.initArgs, debugger?.lastEvaluatedMetadata)
            }
        })
    }

    fun evaluate(env: CastingEnvironment, pattern: HexPattern) = evaluate(env, PatternIota(pattern))

    fun evaluate(env: CastingEnvironment, iota: Iota) = evaluate(env, SpellList.LList(listOf(iota)))

    fun evaluate(env: CastingEnvironment, list: SpellList) = debugger?.let {
        val result = it.evaluate(env, list)
        if (result.startedEvaluating) {
            setEvaluatorState(EvaluatorItem.EvalState.MODIFIED)
        }
        handleDebuggerStep(result)
    }

    fun resetEvaluator() {
        setEvaluatorState(EvaluatorItem.EvalState.DEFAULT)
        if (debugger?.resetEvaluator() == true) {
            sendStoppedEvent(StopReason.STEP)
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

    private fun handleDebuggerStep(result: DebugStepResult): ExecutionClientView? {
        val view = result.clientInfo?.also {
            MsgEvaluatorClientInfoS2C(it).sendToPlayer(player)
        }

        // TODO: set nonzero exit code if we hit a mishap
        if (result.isDone) {
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

        debugger?.getNextIotaToEvaluate()?.also { (iota, index) ->
            printDebuggerStatus(iota, index)
        }

        return view
    }

    private fun sendStoppedEvent(reason: StopReason) {
        remoteProxy.stopped(StoppedEventArguments().also {
            it.threadId = 0
            it.reason = reason.value
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
        state.initArgs = args
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
        state.apply {
            isConnected = true
            launchArgs = LaunchArgs(args)
        }
        remoteProxy.initialized()
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
        if (isDebugging) sendStoppedEvent(StopReason.STEP)
        return futureOf()
    }

    // stepping

    override fun next(args: NextArguments?): CompletableFuture<Void> {
        debugger?.executeUntilStopped(RequestStepType.OVER)?.also(::handleDebuggerStep)
        return futureOf()
    }

    override fun continue_(args: ContinueArguments?): CompletableFuture<ContinueResponse> {
        debugger?.executeUntilStopped()?.also(::handleDebuggerStep)
        return futureOf()
    }

    override fun stepIn(args: StepInArguments?): CompletableFuture<Void> {
        debugger?.executeOnce()?.also(::handleDebuggerStep)
        return futureOf()
    }

    override fun stepOut(args: StepOutArguments?): CompletableFuture<Void> {
        debugger?.executeUntilStopped(RequestStepType.OUT)?.also(::handleDebuggerStep)
        return futureOf()
    }

    override fun pause(args: PauseArguments): CompletableFuture<Void> {
        // TODO: wisps/circles?
        return futureOf()
    }

    override fun restart(args: RestartArguments?): CompletableFuture<Void> {
        state = NotDebugging(state)
        state.restartArgs?.run(::startDebugging)
        return futureOf()
    }

    override fun terminate(args: TerminateArguments?): CompletableFuture<Void> {
        debugger?.let { debugger ->
            for (source in debugger.getSources()) {
                remoteProxy.loadedSource(LoadedSourceEventArguments().also {
                    it.source = source
                    it.reason = LoadedSourceEventArgumentsReason.REMOVED
                })
            }
        }
        remoteProxy.invalidated(InvalidatedEventArguments())
        remoteProxy.exited(ExitedEventArguments().also { it.exitCode = 0 })
        state = NotDebugging(state)
        return futureOf()
    }

    override fun disconnect(args: DisconnectArguments?): CompletableFuture<Void> {
        state.isConnected = false
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
