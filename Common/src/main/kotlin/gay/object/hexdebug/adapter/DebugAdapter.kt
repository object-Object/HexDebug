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
import gay.`object`.hexdebug.config.HexDebugServerConfig
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

class DebugAdapter(val player: ServerPlayer) : IDebugProtocolServer {
    private var state: DebugAdapterState = NotDebugging()

    private val exceptionBreakpoints = mutableSetOf<ExceptionBreakpointType>()

    private val debuggers get() = (state as? Debugging)?.debuggers

    val launcher: IHexDebugLauncher by lazy {
        DebugProxyServerLauncher.createLauncher(this, ::messageWrapper, ::exceptionHandler)
    }

    private val remoteProxy: IDebugProtocolClient get() = launcher.remoteProxy

    fun isDebugging(threadId: Int) = debugger(threadId) != null

    fun debugger(threadId: Int) = debuggers?.get(threadId)

    private fun packId(threadId: Int, reference: Int): Int =
        (threadId shl THREAD_ID_SHIFT) or (reference and REFERENCE_MASK)

    private fun unpackId(id: Int): Pair<Int, Int> =
        Pair(
            id ushr THREAD_ID_SHIFT, // threadId
            id and REFERENCE_MASK, // reference
        )

    private fun closeEvaluator(threadId: Int?) {
        val info = ExecutionClientView(true, ResolvedPatternType.EVALUATED, listOf(), null)
        MsgEvaluatorClientInfoS2C(threadId, info).sendToPlayer(player)
    }

    private fun setEvaluatorState(threadId: Int, evalState: EvaluatorItem.EvalState) {
        MsgEvaluatorStateS2C(threadId, evalState).sendToPlayer(player)
    }

    private fun printDebuggerStatus(iota: String, index: Int) {
        MsgPrintDebuggerStatusS2C(
            iota = iota,
            index = index,
            line = state.initArgs.indexToLine(index),
            isConnected = state.isConnected,
        ).sendToPlayer(player)
    }

    fun startDebugging(threadId: Int, args: CastArgs, notifyClient: Boolean = true): Boolean {
        val threadLimit = if (args.env.isEnlightened) HexDebugServerConfig.config.maxDebugThreads else 1
        if (threadId >= threadLimit) return false

        val debugger = when (val state = state) {
            is Debugging -> {
                state.restartArgs[threadId] = args
                HexDebugger(threadId, exceptionBreakpoints, state.initArgs, state.launchArgs, args).also {
                    state.debuggers[threadId] = it
                }
            }
            is NotDebugging -> {
                val newState = Debugging(threadId, exceptionBreakpoints, state, args)
                this.state = newState
                newState.debuggers[threadId]!!
            }
        }

        remoteProxy.thread(ThreadEventArguments().also {
            it.reason = ThreadEventArgumentsReason.STARTED
            it.threadId = threadId
        })

        if (notifyClient) {
            MsgDebuggerStateS2C(threadId, DebuggerItem.DebugState.DEBUGGING).sendToPlayer(player)
            closeEvaluator(threadId)
        }

        handleDebuggerStep(threadId, debugger.start())
        return true
    }

    fun disconnectClient() {
        if (state.isConnected) {
            remoteProxy.terminated(TerminatedEventArguments())
            state.isConnected = false
        }
    }

    fun print(
        threadId: Int,
        value: String,
        category: String = OutputEventArgumentsCategory.STDOUT,
        withSource: Boolean = true,
    ) {
        remoteProxy.output(OutputEventArguments().also {
            it.category = category
            it.output = value
            if (withSource) {
                it.setSourceAndPosition(state.initArgs, debugger(threadId)?.lastEvaluatedMetadata)
            }
        })
    }

    fun evaluate(threadId: Int, env: CastingEnvironment, pattern: HexPattern) =
        evaluate(threadId, env, PatternIota(pattern))

    fun evaluate(threadId: Int, env: CastingEnvironment, iota: Iota) =
        evaluate(threadId, env, SpellList.LList(listOf(iota)))

    fun evaluate(threadId: Int, env: CastingEnvironment, list: SpellList) =
        debugger(threadId)?.let {
            val result = it.evaluate(env, list)
            if (result.startedEvaluating) {
                setEvaluatorState(threadId, EvaluatorItem.EvalState.MODIFIED)
            }
            handleDebuggerStep(threadId, result)
        }

    fun resetEvaluator(threadId: Int) {
        setEvaluatorState(threadId, EvaluatorItem.EvalState.DEFAULT)
        if (debugger(threadId)?.resetEvaluator() == true) {
            sendStoppedEvent(threadId, StopReason.STEP)
        }
    }

    fun restartThread(threadId: Int) {
        remoteProxy.thread(ThreadEventArguments().also {
            it.reason = ThreadEventArgumentsReason.EXITED
            it.threadId = threadId
        })
        state.restartArgs[threadId]?.let { startDebugging(threadId, it) }
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

    private fun handleDebuggerStep(threadId: Int, result: DebugStepResult): ExecutionClientView? {
        val view = result.clientInfo?.also {
            MsgEvaluatorClientInfoS2C(threadId, it).sendToPlayer(player)
        }

        // TODO: set nonzero exit code if we hit a mishap
        if (result.isDone) {
            terminateThreads(TerminateThreadsArguments().also {
                it.threadIds = intArrayOf(threadId)
            })
            return view
        }

        for ((source, reason) in result.loadedSources) {
            remoteProxy.loadedSource(LoadedSourceEventArguments().also {
                it.source = source.apply {
                    sourceReference = packId(threadId, sourceReference)
                }
                it.reason = reason
            })
        }

        sendStoppedEvent(threadId, result.reason)

        debugger(threadId)?.getNextIotaToEvaluate()?.also { (iota, index) ->
            printDebuggerStatus(iota, index)
        }

        return view
    }

    private fun sendStoppedEvent(threadId: Int?, reason: StopReason) {
        if (threadId == null) {
            debuggers?.keys?.forEach { sendStoppedEvent(it, reason) }
        } else {
            remoteProxy.stopped(StoppedEventArguments().also {
                it.threadId = threadId
                it.reason = reason.value
            })
        }
    }

    private fun invalidateBreakpoints(n: Int) = Array(n) {
        Breakpoint().apply {
            isVerified = false
            message = "Invalid"
            reason = BreakpointNotVerifiedReason.FAILED
        }
    }

    private fun resumeAllExcept(threadId: Int) {
        for (index in allThreadIds) {
            val debugger = debugger(index)
            if (debugger != null && index != threadId) {
                handleDebuggerStep(threadId, debugger.executeUntilStopped())
            }
        }
    }

    // initialization

    override fun initialize(args: InitializeRequestArguments): CompletableFuture<Capabilities> {
        state.initArgs = args
        return Capabilities().apply {
            supportsConfigurationDoneRequest = true
            supportsLoadedSourcesRequest = true
            supportsTerminateRequest = true
            supportsTerminateThreadsRequest = true
            supportsSingleThreadExecutionRequests = true
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
            val (threadId, sourceReference) = unpackId(args.source.sourceReference)
            val debugger = debugger(threadId)
            breakpoints = if (debugger != null && player.uuid in knownPlayers) {
                args.breakpoints
                    .map { debugger.setBreakpoint(sourceReference, it) }
                    .toTypedArray()
            } else {
                invalidateBreakpoints(args.breakpoints.size)
            }
        }.toFuture()
    }

    override fun setExceptionBreakpoints(args: SetExceptionBreakpointsArguments): CompletableFuture<SetExceptionBreakpointsResponse> {
        exceptionBreakpoints.clear()
        val responseBreakpoints = mutableListOf<Breakpoint>()

        for (name in args.filters) {
            val verified = try {
                exceptionBreakpoints.add(ExceptionBreakpointType.valueOf(name))
                true
            } catch (_: IllegalArgumentException) {
                false
            }
            responseBreakpoints.add(Breakpoint().apply { isVerified = verified })
        }

        return SetExceptionBreakpointsResponse().apply {
            breakpoints = responseBreakpoints.toTypedArray()
        }.toFuture()
    }

    override fun configurationDone(args: ConfigurationDoneArguments?): CompletableFuture<Void> {
        knownPlayers.add(player.uuid)
        if (state is Debugging) sendStoppedEvent(null, StopReason.STEP)
        return futureOf()
    }

    // stepping

    override fun next(args: NextArguments): CompletableFuture<Void> {
        debugger(args.threadId)?.let {
            handleDebuggerStep(args.threadId, it.executeUntilStopped(RequestStepType.OVER))
        }
        // HACK: vscode doesn't support single-thread execution
        // so just always default to single-thread unless explicitly requested otherwise
        // this is the opposite of what the spec says, hopefully that doesn't break anything :3
        // https://github.com/microsoft/vscode/issues/166450
        if (args.singleThread == false) { // this can be null!
            resumeAllExcept(args.threadId)
        }
        return futureOf()
    }

    override fun continue_(args: ContinueArguments): CompletableFuture<ContinueResponse> {
        debugger(args.threadId)?.let {
            handleDebuggerStep(args.threadId, it.executeUntilStopped())
        }
        val continueAllThreads = args.singleThread == false
        if (continueAllThreads) {
            resumeAllExcept(args.threadId)
        }
        return ContinueResponse().apply {
            allThreadsContinued = continueAllThreads
        }.toFuture()
    }

    override fun stepIn(args: StepInArguments): CompletableFuture<Void> {
        debugger(args.threadId)?.let {
            handleDebuggerStep(args.threadId, it.executeOnce())
        }
        if (args.singleThread == false) {
            resumeAllExcept(args.threadId)
        }
        return futureOf()
    }

    override fun stepOut(args: StepOutArguments): CompletableFuture<Void> {
        debugger(args.threadId)?.let {
            handleDebuggerStep(args.threadId, it.executeUntilStopped(RequestStepType.OUT))
        }
        if (args.singleThread == false) {
            resumeAllExcept(args.threadId)
        }
        return futureOf()
    }

    override fun pause(args: PauseArguments): CompletableFuture<Void> {
        // TODO: wisps/circles?
        return futureOf()
    }

    override fun restart(args: RestartArguments?): CompletableFuture<Void> {
        state = NotDebugging(state)

        for ((threadId, castArgs) in state.restartArgs) {
            startDebugging(threadId, castArgs, false)
        }

        MsgDebuggerStateS2C(
            allThreadIds.associateWith {
                DebuggerItem.DebugState.of(isDebugging(it))
            }
        ).sendToPlayer(player)

        closeEvaluator(null)

        return futureOf()
    }

    override fun terminateThreads(args: TerminateThreadsArguments): CompletableFuture<Void> {
        val state = (state as? Debugging) ?: return futureOf()

        for (threadId in args.threadIds) {
            // notify the client
            remoteProxy.thread(ThreadEventArguments().also {
                it.reason = ThreadEventArgumentsReason.EXITED
                it.threadId = threadId
            })

            state.debuggers.remove(threadId)?.let { debugger ->
                for (source in debugger.getSources()) {
                    remoteProxy.loadedSource(LoadedSourceEventArguments().also {
                        it.source = source
                        it.reason = LoadedSourceEventArgumentsReason.REMOVED
                    })
                }
            }
        }

        MsgDebuggerStateS2C(
            args.threadIds.associateWith {
                DebuggerItem.DebugState.NOT_DEBUGGING
            }
        ).sendToPlayer(player)

        if (state.debuggers.isEmpty()) {
            remoteProxy.exited(ExitedEventArguments().also { it.exitCode = 0 })
            this.state = NotDebugging(state)
            closeEvaluator(null)
        } else {
            for (threadId in args.threadIds) {
                closeEvaluator(threadId)
            }
        }

        return futureOf()
    }

    override fun terminate(args: TerminateArguments?): CompletableFuture<Void> {
        return debuggers?.let { debuggers ->
            terminateThreads(TerminateThreadsArguments().also {
                it.threadIds = debuggers.keys.toIntArray()
            })
        } ?: futureOf()
    }

    override fun disconnect(args: DisconnectArguments?): CompletableFuture<Void> {
        state.isConnected = false
        return futureOf()
    }

    // runtime data

    override fun threads(): CompletableFuture<ThreadsResponse> {
        return ThreadsResponse().apply {
            threads = debuggers
                ?.map { (threadId, debugger) ->
                    Thread().apply {
                        id = threadId
                        name = "Thread $threadId (${debugger.envType})"
                    }
                }
                ?.sortedBy { it.id }
                ?.toTypedArray()
                ?: arrayOf()
        }.toFuture()
    }

    override fun scopes(args: ScopesArguments): CompletableFuture<ScopesResponse> {
        val (threadId, frameId) = unpackId(args.frameId)
        return ScopesResponse().apply {
            scopes = debugger(threadId)
                ?.getScopes(frameId)
                ?.onEach {
                    it.variablesReference = packId(threadId, it.variablesReference)
                    if (it.source != null) {
                        it.source.sourceReference = packId(threadId, it.source.sourceReference)
                    }
                }
                ?.toTypedArray()
                ?: arrayOf()
        }.toFuture()
    }

    override fun variables(args: VariablesArguments): CompletableFuture<VariablesResponse> {
        val (threadId, variablesReference) = unpackId(args.variablesReference)
        return VariablesResponse().apply {
            variables = debugger(threadId)
                ?.getVariables(variablesReference)
                ?.onEach { it.variablesReference = packId(threadId, it.variablesReference) }
                ?.paginate(args.start, args.count)
                ?: arrayOf()
        }.toFuture()
    }

    override fun stackTrace(args: StackTraceArguments): CompletableFuture<StackTraceResponse> {
        return StackTraceResponse().apply {
            stackFrames = debugger(args.threadId)
                ?.getStackFrames()
                ?.onEach {
                    it.id = packId(args.threadId, it.id)
                    if (it.source != null) {
                        it.source.sourceReference = packId(args.threadId, it.source.sourceReference)
                    }
                }
                ?.paginate(args.startFrame, args.levels)
                ?: arrayOf()
        }.toFuture()
    }

    override fun source(args: SourceArguments): CompletableFuture<SourceResponse> {
        val (threadId, sourceReference) = unpackId(args.source?.sourceReference ?: args.sourceReference)
        return SourceResponse().apply {
            content = debugger(threadId)?.getSourceContents(sourceReference) ?: ""
        }.toFuture()
    }

    override fun loadedSources(args: LoadedSourcesArguments?): CompletableFuture<LoadedSourcesResponse> {
        return LoadedSourcesResponse().apply {
            sources = debuggers
                ?.flatMap { (threadId, debugger) ->
                    debugger.getSources().onEach {
                        it.sourceReference = packId(threadId, it.sourceReference)
                    }
                }
                ?.toTypedArray()
                ?: arrayOf()
        }.toFuture()
    }

    companion object {
        private const val THREAD_ID_SHIFT = 32 - HexDebugServerConfig.THREAD_BITS
        private const val REFERENCE_MASK = -1 ushr THREAD_ID_SHIFT

        // set of players that have connected since the server started
        // this is used to invalidate old client-side debugger data if necessary
        private val knownPlayers = mutableSetOf<UUID>()

        private val allThreadIds get() = 0 until HexDebugServerConfig.config.maxDebugThreads
    }
}
