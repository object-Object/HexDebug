package gay.`object`.hexdebug.adapter

import at.petrak.hexcasting.api.casting.SpellList
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.ExecutionClientView
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexPattern
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.adapter.proxy.DebugProxyServerLauncher
import gay.`object`.hexdebug.config.HexDebugServerConfig
import gay.`object`.hexdebug.core.api.debugging.StopReason
import gay.`object`.hexdebug.core.api.debugging.env.DebugEnvironment
import gay.`object`.hexdebug.core.api.exceptions.IllegalDebugSessionException
import gay.`object`.hexdebug.core.api.exceptions.IllegalDebugThreadException
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
    private var isConnected = false
    private var lastDebugger: HexDebugger? = null // for resolving sources after exit
    private val debuggers = mutableMapOf<Int, HexDebugger>()
    private val threadIds = mutableMapOf<UUID, Int>()
    private val state = SharedDebugState()

    val launcher: IHexDebugLauncher by lazy {
        DebugProxyServerLauncher.createLauncher(this, ::messageWrapper, ::exceptionHandler)
    }

    private val remoteProxy: IDebugProtocolClient get() = launcher.remoteProxy

    private val maxThreads get() = HexDebugServerConfig.config.maxDebugThreads(player)

    fun isDebugging(threadId: Int) = debugger(threadId) != null

    fun debugger(sessionId: UUID) =
        threadIds[sessionId]
            ?.let(debuggers::get)
            ?.takeIf { it.sessionId == sessionId }

    fun debugger(threadId: Int) = debuggers[threadId]

    private fun inRangeDebugger(threadId: Int) =
        debugger(threadId)?.takeIf { it.debugEnv.isCasterInRange }

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
            isConnected = isConnected,
        ).sendToPlayer(player)
    }

    fun createDebugThread(debugEnv: DebugEnvironment, maybeThreadId: Int?) {
        if (debugEnv.sessionId in threadIds) {
            throw IllegalDebugSessionException("Debug session already in use")
        }

        val threadId = resolveThreadId(maybeThreadId)
        threadIds[debugEnv.sessionId] = threadId

        val debugger = HexDebugger(state, debugEnv, threadId)
        debuggers[threadId] = debugger

        remoteProxy.thread(ThreadEventArguments().also {
            it.reason = ThreadEventArgumentsReason.STARTED
            it.threadId = threadId
        })

        MsgDebuggerStateS2C(threadId, DebuggerItem.DebugState.DEBUGGING).sendToPlayer(player)
        closeEvaluator(threadId)
    }

    private fun resolveThreadId(threadId: Int?): Int {
        if (threadId == null) {
            return (0 until maxThreads).firstOrNull { !isDebugging(it) }
                ?: throw IllegalDebugThreadException("All debug threads are already in use")
        }

        if (threadId !in (0 until maxThreads)) {
            throw IllegalDebugThreadException("Debug thread ID out of range: $threadId")
        }

        if (isDebugging(threadId)) {
            throw IllegalDebugThreadException("Debug thread already in use: $threadId")
        }

        return threadId
    }

    fun startExecuting(
        debugEnv: DebugEnvironment,
        env: CastingEnvironment,
        iotas: List<Iota>,
        image: CastingImage?,
    ) {
        val debugger = debugger(debugEnv.sessionId)
            ?: throw IllegalDebugSessionException("Debug session not found")

        val result = debugger.startExecuting(env, iotas, image)
            ?: throw IllegalDebugSessionException("Debug session is already executing something")

        lastDebugger = debugger
        handleDebuggerStep(debugger.threadId, result, wasPaused = false)
    }

    fun onRemove() {
        if (isConnected) {
            remoteProxy.terminated(TerminatedEventArguments())
            isConnected = false
        }
        forceTerminateAll()
    }

    fun onDeath() {
        forceTerminateAll()
    }

    private fun forceTerminateAll() {
        val debugEnvs = debuggers.values.map { it.debugEnv }
        debuggers.clear()
        threadIds.clear()
        for (debugEnv in debugEnvs) {
            debugEnv.terminate()
        }
    }

    fun print(
        sessionId: UUID,
        value: String,
        category: String = OutputEventArgumentsCategory.STDOUT,
        withSource: Boolean = true,
    ) {
        val debugger = debugger(sessionId) ?: return
        remoteProxy.output(OutputEventArguments().also {
            it.category = category
            it.output = value
            if (withSource) {
                it.setSourceAndPosition(state.initArgs, debugger.lastEvaluatedMetadata)
            }
        })
    }

    fun evaluate(threadId: Int, pattern: HexPattern) =
        evaluate(threadId, PatternIota(pattern))

    fun evaluate(threadId: Int, iota: Iota) =
        evaluate(threadId, SpellList.LList(listOf(iota)))

    fun evaluate(threadId: Int, list: SpellList) =
        inRangeDebugger(threadId)?.let {
            val result = it.evaluate(list) ?: return null
            if (result.startedEvaluating) {
                setEvaluatorState(threadId, EvaluatorItem.EvalState.MODIFIED)
            }
            handleDebuggerStep(threadId, result)
        }

    fun resetEvaluator(threadId: Int) {
        setEvaluatorState(threadId, EvaluatorItem.EvalState.DEFAULT)
        if (inRangeDebugger(threadId)?.resetEvaluator() == true) {
            sendStoppedEvent(threadId, StopReason.STEP)
        }
    }

    fun restartThread(threadId: Int) {
        remoteProxy.thread(ThreadEventArguments().also {
            it.reason = ThreadEventArgumentsReason.EXITED
            it.threadId = threadId
        })
        debuggers.remove(threadId)?.let {
            threadIds.remove(it.sessionId)
            it.debugEnv.restart(threadId)
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

    private fun handleDebuggerStep(
        threadId: Int,
        result: DebugStepResult,
        wasPaused: Boolean = true,
        sendContinuedEvent: Boolean = true,
    ): ExecutionClientView? {
        val view = result.clientInfo?.also {
            MsgEvaluatorClientInfoS2C(threadId, it).sendToPlayer(player)
        }

        // TODO: set nonzero exit code if we hit a mishap
        if (result.isDone) {
            terminateThreads(listOf(threadId))
            return view
        }

        for ((source, reason) in result.loadedSources) {
            remoteProxy.loadedSource(LoadedSourceEventArguments().also {
                it.source = source
                it.reason = reason
            })
        }

        if (result.reason != null) {
            // stopped
            sendStoppedEvent(threadId, result.reason)

            debugger(threadId)?.getNextIotaToEvaluate()?.also { (iota, index) ->
                printDebuggerStatus(iota, index)
            }
        } else if (wasPaused) {
            // running
            if (sendContinuedEvent) {
                remoteProxy.continued(ContinuedEventArguments().also {
                    it.threadId = threadId
                    it.allThreadsContinued = false
                })
            }

            closeEvaluator(threadId)
        }

        return view
    }

    private fun sendStoppedEvent(threadId: Int?, reason: StopReason) {
        if (threadId == null) {
            debuggers.keys.forEach { sendStoppedEvent(it, reason) }
        } else {
            val reasonStr = when (reason) {
                StopReason.STEP -> StoppedEventArgumentsReason.STEP
                StopReason.PAUSE -> StoppedEventArgumentsReason.PAUSE
                StopReason.BREAKPOINT -> StoppedEventArgumentsReason.BREAKPOINT
                StopReason.EXCEPTION -> StoppedEventArgumentsReason.EXCEPTION
                StopReason.STARTED -> StoppedEventArgumentsReason.ENTRY
                StopReason.TERMINATED -> return
            }
            remoteProxy.stopped(StoppedEventArguments().also {
                it.threadId = threadId
                it.reason = reasonStr
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

    private fun resumeAllExcept(threadId: Int, sendContinuedEvent: Boolean = true) {
        for (index in allThreadIds) {
            if (index == threadId) continue
            inRangeDebugger(index)?.let {
                handleDebuggerStep(threadId, it.executeUntilStopped(), sendContinuedEvent = sendContinuedEvent)
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
        isConnected = true
        state.launchArgs = LaunchArgs(args)
        remoteProxy.initialized()
        player.displayClientMessage(Component.translatable("text.hexdebug.debugging.connected"), true)
        return futureOf()
    }

    // configuration

    override fun setBreakpoints(args: SetBreakpointsArguments): CompletableFuture<SetBreakpointsResponse> {
        return SetBreakpointsResponse().apply {
            // source prefixes generally invalidate when the server restarts
            // so remove all breakpoints if we haven't seen this player before
            breakpoints = if (player.uuid in knownPlayers) {
                state.setBreakpoints(args.source.sourceReference, args.breakpoints).toTypedArray()
            } else {
                invalidateBreakpoints(args.breakpoints.size)
            }
        }.toFuture()
    }

    override fun setExceptionBreakpoints(args: SetExceptionBreakpointsArguments): CompletableFuture<SetExceptionBreakpointsResponse> {
        state.exceptionBreakpoints.clear()
        val responseBreakpoints = mutableListOf<Breakpoint>()

        for (name in args.filters) {
            val verified = try {
                state.exceptionBreakpoints.add(ExceptionBreakpointType.valueOf(name))
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
        if (debuggers.isNotEmpty()) sendStoppedEvent(null, StopReason.STEP)
        return futureOf()
    }

    // stepping

    override fun next(args: NextArguments): CompletableFuture<Void> {
        inRangeDebugger(args.threadId)?.let {
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
        inRangeDebugger(args.threadId)?.let {
            handleDebuggerStep(args.threadId, it.executeUntilStopped(), sendContinuedEvent = false)
        }
        val continueAllThreads = args.singleThread == false
        if (continueAllThreads) {
            resumeAllExcept(args.threadId, sendContinuedEvent = false)
        }
        return ContinueResponse().apply {
            allThreadsContinued = continueAllThreads
        }.toFuture()
    }

    override fun stepIn(args: StepInArguments): CompletableFuture<Void> {
        inRangeDebugger(args.threadId)?.let {
            handleDebuggerStep(args.threadId, it.executeUntilStopped(RequestStepType.IN))
        }
        if (args.singleThread == false) {
            resumeAllExcept(args.threadId)
        }
        return futureOf()
    }

    override fun stepOut(args: StepOutArguments): CompletableFuture<Void> {
        inRangeDebugger(args.threadId)?.let {
            handleDebuggerStep(args.threadId, it.executeUntilStopped(RequestStepType.OUT))
        }
        if (args.singleThread == false) {
            resumeAllExcept(args.threadId)
        }
        return futureOf()
    }

    override fun pause(args: PauseArguments): CompletableFuture<Void> {
        inRangeDebugger(args.threadId)?.pause()
        return futureOf()
    }

    override fun restart(args: RestartArguments?): CompletableFuture<Void> {
        // hack
        for (debugger in debuggers.values) {
            if (!debugger.debugEnv.isCasterInRange) {
                return futureOf()
            }
        }

        val envs = debuggers.map { (threadId, debugger) -> threadId to debugger.debugEnv }

        debuggers.clear()
        threadIds.clear()

        for ((threadId, debugEnv) in envs) {
            debugEnv.restart(threadId)
        }

        MsgDebuggerStateS2C(
            allThreadIds.associateWith {
                DebuggerItem.DebugState.of(isDebugging(it))
            }
        ).sendToPlayer(player)

        closeEvaluator(null)

        return futureOf()
    }

    fun removeThread(sessionId: UUID, terminate: Boolean) {
        threadIds[sessionId]?.let {
            removeThreadInner(it, terminate)
            postRemoveThreads(listOf(it))
        }
    }

    override fun terminateThreads(args: TerminateThreadsArguments): CompletableFuture<Void> {
        val toRemove = args.threadIds.filter { inRangeDebugger(it) != null }
        if (toRemove.isEmpty()) return futureOf()

        terminateThreads(toRemove)

        return futureOf()
    }

    private fun terminateThreads(toRemove: List<Int>) {
        for (threadId in toRemove) {
            removeThreadInner(threadId, terminate = true)
        }
        postRemoveThreads(toRemove)
    }

    private fun removeThreadInner(threadId: Int, terminate: Boolean) {
        remoteProxy.thread(ThreadEventArguments().also {
            it.reason = ThreadEventArgumentsReason.EXITED
            it.threadId = threadId
        })

        debuggers.remove(threadId)?.let {
            threadIds.remove(it.sessionId)
            if (terminate) it.debugEnv.terminate()
        }
    }

    private fun postRemoveThreads(threadIds: List<Int>) {
        MsgDebuggerStateS2C(
            threadIds.associateWith {
                DebuggerItem.DebugState.NOT_DEBUGGING
            }
        ).sendToPlayer(player)

        if (debuggers.isEmpty()) {
            if (isConnected) {
                remoteProxy.exited(ExitedEventArguments().also { it.exitCode = 0 })
            } else {
                lastDebugger = null
                state.sourceAllocator.clear()
            }
            closeEvaluator(null)
        } else {
            for (threadId in threadIds) {
                closeEvaluator(threadId)
            }
        }
    }

    override fun terminate(args: TerminateArguments?): CompletableFuture<Void> {
        return terminateThreads(TerminateThreadsArguments().also {
            it.threadIds = debuggers.keys.toIntArray()
        })
    }

    override fun disconnect(args: DisconnectArguments?): CompletableFuture<Void> {
        isConnected = false
        state.onDisconnect()
        // if there's an active debug session, we need to keep the sources in case the client reconnects
        if (debuggers.isEmpty()) {
            lastDebugger = null
            state.sourceAllocator.clear()
        }
        return futureOf()
    }

    // runtime data

    override fun threads(): CompletableFuture<ThreadsResponse> {
        return ThreadsResponse().apply {
            threads = debuggers
                .map { (threadId, debugger) ->
                    Thread().apply {
                        id = threadId
                        name = "Thread $threadId (${debugger.debugEnv.name.string})"
                    }
                }
                .sortedBy { it.id }
                .toTypedArray()
        }.toFuture()
    }

    override fun scopes(args: ScopesArguments): CompletableFuture<ScopesResponse> {
        val (threadId, frameId) = unpackId(args.frameId)
        return ScopesResponse().apply {
            scopes = debugger(threadId)
                ?.getScopes(frameId)
                ?.onEach { it.variablesReference = packId(threadId, it.variablesReference) }
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
                ?.onEach { it.id = packId(args.threadId, it.id) }
                ?.paginate(args.startFrame, args.levels)
                ?: arrayOf()
        }.toFuture()
    }

    override fun source(args: SourceArguments): CompletableFuture<SourceResponse> {
        val debugger = (args.source?.adapterData as? Int)?.let(::debugger) ?: lastDebugger
        val sourceReference = args.source?.sourceReference ?: args.sourceReference
        return SourceResponse().apply {
            content = debugger?.getSourceContents(sourceReference) ?: ""
        }.toFuture()
    }

    override fun loadedSources(args: LoadedSourcesArguments?): CompletableFuture<LoadedSourcesResponse> {
        return LoadedSourcesResponse().apply {
            sources = state.getSources().toTypedArray()
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
