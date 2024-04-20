package ca.objectobject.hexdebug.server

import ca.objectobject.hexdebug.HexDebug
import ca.objectobject.hexdebug.debugger.DebugCastArgs
import ca.objectobject.hexdebug.debugger.HexDebugger
import ca.objectobject.hexdebug.debugger.StopType
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import org.eclipse.lsp4j.debug.*
import org.eclipse.lsp4j.debug.launch.DSPLauncher
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

/*
TODO:
- auto-evaluate all non-eval frames
- line number is super wrong
- program source not refreshing??
 */

class HexDebugServer(
    input: InputStream,
    output: OutputStream,
    private var queuedCast: DebugCastArgs? = null,
) : IDebugProtocolServer {
    constructor(clientSocket: Socket, queuedCast: DebugCastArgs? = null) : this(
        clientSocket.inputStream,
        clientSocket.outputStream,
        queuedCast,
    )

    private val launcher = DSPLauncher.createServerLauncher(this, input, output)
    private var future: Future<Void>? = null

    private val remoteProxy: IDebugProtocolClient get() = launcher.remoteProxy

    public var state: HexDebugServerState = HexDebugServerState.NOT_READY

    private lateinit var initArgs: InitializeRequestArguments
    private lateinit var launchArgs: LaunchArgs
    private lateinit var debugger: HexDebugger

    fun start(): Future<Void> {
        return launcher.startListening().also { future = it }
    }

    fun stop(notifyClient: Boolean = true) {
        state = HexDebugServerState.CLOSED
        HexDebug.LOGGER.info("Stopping debug server")

        future?.cancel(true)

        if (notifyClient) {
            remoteProxy.exited(ExitedEventArguments().also { it.exitCode = 0 })
            remoteProxy.terminated(TerminatedEventArguments())
        }
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
        }.toFuture()
    }

    override fun attach(args: MutableMap<String, Any>): CompletableFuture<Void> {
        logRequest("attach", args)

        launchArgs = LaunchArgs(args)
        state = HexDebugServerState.READY

        queuedCast?.also(::startDebugging)
        queuedCast = null

        return futureOf()
    }

    // not a request
    fun startDebugging(cast: DebugCastArgs) = when (state) {
        HexDebugServerState.NOT_READY -> {
            queuedCast = cast
            (cast.vm.env.castingEntity as? ServerPlayer)?.sendSystemMessage(
                Component.translatable("text.hexdebug.no_client"), false
            )
            true
        }
        HexDebugServerState.READY -> {
            state = HexDebugServerState.DEBUGGING
            debugger = HexDebugger(initArgs, launchArgs, cast)
            (cast.vm.env.castingEntity as? ServerPlayer)?.sendSystemMessage(
                Component.translatable("text.hexdebug.connected"), false
            )
            remoteProxy.initialized()
            true
        }
        else -> {
            HexDebug.LOGGER.info("Debugger is already started, cancelling")
            false
        }
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
//        } else if (debugger.isAtBreakpoint) {
//            sendStoppedEvent("breakpoint")
        } else {
            handleDebuggerStep(debugger.executeUntilStopped())
        }
        return futureOf()
    }

    override fun next(args: NextArguments?): CompletableFuture<Void> {
        logRequest("next", args)
        handleDebuggerStep(debugger.executeUntilStopped(StopType.STEP_OVER))
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
        handleDebuggerStep(debugger.executeUntilStopped(StopType.STEP_OUT))
        return futureOf()
    }

    override fun pause(args: PauseArguments): CompletableFuture<Void> {
        logRequest("pause", args)
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

    // helpers

    private fun handleDebuggerStep(reason: String?) {
        if (reason != null) {
            sendStoppedEvent(reason)
//            if (debugger.currentLineNumber == 0) {
//                // we stepped into a nested eval; refresh all the sources
//                for (source in debugger.getSources()) {
//                    remoteProxy.loadedSource(LoadedSourceEventArguments().also {
//                        it.reason = LoadedSourceEventArgumentsReason.NEW
//                        it.source = source
//                    })
//                }
//            }
        } else {
            HexDebug.LOGGER.info("Program exited, stopping debug server")
            stop()
        }
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

fun <T> T.toFuture(): CompletableFuture<T> = CompletableFuture.completedFuture(this)

fun <T> futureOf(value: T): CompletableFuture<T> = CompletableFuture.completedFuture(value)

fun <T> futureOf(): CompletableFuture<T> = CompletableFuture.completedFuture(null)

inline fun <reified T> Sequence<T>.paginate(start: Int?, count: Int?): Array<T> {
    var result = this
    if (start != null && start > 0) {
        result = result.drop(start)
    }
    if (count != null && count > 0) {
        result = result.take(count)
    }
    return result.toList().toTypedArray()
}
