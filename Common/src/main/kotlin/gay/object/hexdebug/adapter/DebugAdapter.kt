package gay.`object`.hexdebug.adapter

import at.petrak.hexcasting.api.casting.eval.env.PlayerBasedCastEnv
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.iota.Iota
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.debugger.DebugStepResult
import gay.`object`.hexdebug.debugger.HexDebugger
import gay.`object`.hexdebug.debugger.RequestStepType
import gay.`object`.hexdebug.utils.futureOf
import gay.`object`.hexdebug.utils.paginate
import gay.`object`.hexdebug.utils.toFuture
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import org.eclipse.lsp4j.debug.*
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer
import org.eclipse.lsp4j.jsonrpc.Launcher
import java.util.concurrent.CompletableFuture

class DebugAdapter(
    private val iotas: List<Iota>,
    private val env: PlayerBasedCastEnv,
    private val world: ServerLevel,
    private val onExecute: ((Iota) -> Unit)? = null,
) : IDebugProtocolServer {
    lateinit var launcher: Launcher<IDebugProtocolClient>

    private lateinit var initArgs: InitializeRequestArguments
    private lateinit var launchArgs: LaunchArgs
    private lateinit var debugger: HexDebugger

    private val remoteProxy: IDebugProtocolClient get() = launcher.remoteProxy

    var isTerminated = false

    fun stop(notifyClient: Boolean = true) {
        HexDebug.LOGGER.info("Stopping debug adapter")

        if (notifyClient) {
            remoteProxy.exited(ExitedEventArguments().also { it.exitCode = 0 })
            remoteProxy.terminated(TerminatedEventArguments())
        }

        isTerminated = true
    }

    fun print(value: String, category: String = OutputEventArgumentsCategory.STDOUT) {
        remoteProxy.output(OutputEventArguments().also {
            it.category = category
            it.output = value
        })
    }

    // lifecycle requests

    override fun initialize(args: InitializeRequestArguments): CompletableFuture<Capabilities> {
        logRequest("initialize", args)

        initArgs = args

        return Capabilities().apply {
            supportsConfigurationDoneRequest = true
            supportsLoadedSourcesRequest = true
            supportsTerminateRequest = true
        }.toFuture()
    }

    override fun attach(args: MutableMap<String, Any>): CompletableFuture<Void> {
        logRequest("attach", args)

        launchArgs = LaunchArgs(args)
        debugger = createDebugger()
        env.caster?.displayClientMessage(Component.translatable("text.hexdebug.connected"), true)

        remoteProxy.initialized()
        return futureOf()
    }

    override fun setBreakpoints(args: SetBreakpointsArguments): CompletableFuture<SetBreakpointsResponse> {
        logRequest("setBreakpoints", args)
        return SetBreakpointsResponse().apply {
            breakpoints = debugger.setBreakpoints(args.source.sourceReference, args.breakpoints).toTypedArray()
        }.toFuture()
    }

    override fun setExceptionBreakpoints(args: SetExceptionBreakpointsArguments): CompletableFuture<SetExceptionBreakpointsResponse> {
        logRequest("setExceptionBreakpoints", args)

        // tell the client we didn't enable any of their breakpoints
        val count = args.filters.size + (args.filterOptions?.size ?: 0) + (args.exceptionOptions?.size ?: 0)
        val breakpoints = Array(count) { Breakpoint().apply { isVerified = false } }

        return SetExceptionBreakpointsResponse().apply {
            this.breakpoints = breakpoints
        }.toFuture()
    }

    override fun configurationDone(args: ConfigurationDoneArguments?): CompletableFuture<Void> {
        logRequest("configurationDone", args)
        if (launchArgs.stopOnEntry) {
            sendStoppedEvent("entry")
        } else if (debugger.isAtBreakpoint) {
            sendStoppedEvent("breakpoint")
        } else {
            handleDebuggerStep(debugger.executeUntilStopped())
        }
        return futureOf()
    }

    override fun next(args: NextArguments?): CompletableFuture<Void> {
        logRequest("next", args)
        handleDebuggerStep(debugger.executeUntilStopped(RequestStepType.OVER))
        return futureOf()
    }

    override fun continue_(args: ContinueArguments): CompletableFuture<ContinueResponse> {
        logRequest("continue", args)
        handleDebuggerStep(debugger.executeUntilStopped())
        return futureOf()
    }

    override fun stepIn(args: StepInArguments?): CompletableFuture<Void> {
        logRequest("stepIn", args)
        handleDebuggerStep(debugger.executeOnce())
        return futureOf()
    }

    override fun stepOut(args: StepOutArguments?): CompletableFuture<Void> {
        logRequest("stepOut", args)
        handleDebuggerStep(debugger.executeUntilStopped(RequestStepType.OUT))
        return futureOf()
    }

    override fun pause(args: PauseArguments): CompletableFuture<Void> {
        logRequest("pause", args)
        return futureOf()
    }

    override fun terminate(args: TerminateArguments?): CompletableFuture<Void> {
        logRequest("terminate", args)
        stop()
        return futureOf()
    }

    override fun disconnect(args: DisconnectArguments): CompletableFuture<Void> {
        logRequest("disconnect", args)
        stop(notifyClient = false)
        return futureOf()
    }

    // runtime data

    override fun threads(): CompletableFuture<ThreadsResponse> {
        logRequest("threads")
        // always return the same dummy thread - we don't support multithreading
        return ThreadsResponse().apply {
            threads = arrayOf(Thread().apply {
                id = 0
                name = "Main Thread"
            })
        }.toFuture()
    }

    override fun scopes(args: ScopesArguments): CompletableFuture<ScopesResponse> {
        logRequest("scopes", args)
        return ScopesResponse().apply {
            scopes = debugger.getScopes(args.frameId).toTypedArray()
        }.toFuture()
    }

    override fun variables(args: VariablesArguments): CompletableFuture<VariablesResponse> {
        logRequest("variables", args)
        return VariablesResponse().apply {
            variables = debugger.getVariables(args.variablesReference).paginate(args.start, args.count)
        }.toFuture()
    }

    override fun stackTrace(args: StackTraceArguments): CompletableFuture<StackTraceResponse> {
        logRequest("stackTrace", args)
        return StackTraceResponse().apply {
            stackFrames = debugger.getStackFrames().paginate(args.startFrame, args.levels)
        }.toFuture()
    }

    override fun source(args: SourceArguments): CompletableFuture<SourceResponse> {
        logRequest("source", args)
        return SourceResponse().apply {
            content = debugger.getSourceContents(args.source.sourceReference)
        }.toFuture()
    }

    override fun loadedSources(args: LoadedSourcesArguments?): CompletableFuture<LoadedSourcesResponse> {
        logRequest("loadedSources", args)
        return LoadedSourcesResponse().apply {
            sources = debugger.getSources().toTypedArray()
        }.toFuture()
    }

    // helpers

    private fun createDebugger() = HexDebugger(initArgs, launchArgs, CastingVM.empty(env), world, onExecute, iotas)

    private fun handleDebuggerStep(result: DebugStepResult?) {
        if (result == null) {
            HexDebug.LOGGER.info("Program exited, stopping debug adapter")
            stop()
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

    private fun logRequest(name: String, args: Any? = null) {
        HexDebug.LOGGER.debug("Got request {} with args {}", name, args)
    }
}


