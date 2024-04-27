package gay.`object`.hexdebug.adapter

import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.adapter.DebugAdapterState.Debugging
import gay.`object`.hexdebug.adapter.DebugAdapterState.NotDebugging
import gay.`object`.hexdebug.adapter.proxy.DebugProxyServerLauncher
import gay.`object`.hexdebug.debugger.DebugStepResult
import gay.`object`.hexdebug.debugger.RequestStepType
import gay.`object`.hexdebug.utils.futureOf
import gay.`object`.hexdebug.utils.paginate
import gay.`object`.hexdebug.utils.toFuture
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import org.apache.commons.lang3.exception.ExceptionUtils
import org.eclipse.lsp4j.debug.*
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer
import org.eclipse.lsp4j.jsonrpc.MessageConsumer
import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode
import java.util.concurrent.CompletableFuture

class DebugAdapter(val player: ServerPlayer) : IDebugProtocolServer {
    val launcher = DebugProxyServerLauncher.createLauncher(this, ::messageWrapper, ::exceptionHandler)

    private val remoteProxy: IDebugProtocolClient get() = launcher.remoteProxy

    private var state: DebugAdapterState = NotDebugging()

    val isDebugging get() = state is Debugging

    fun cast(args: CastArgs) {
        if (state is Debugging) {
            terminate()
        }
        setArgs { castArgs = args }
        if (state is NotDebugging) {
            player.displayClientMessage(Component.translatable("text.hexdebug.no_client"), true)
        }
    }

    fun terminate(notifyClient: Boolean = true, restart: Boolean = false) {
        if (notifyClient) {
            remoteProxy.exited(ExitedEventArguments().also { it.exitCode = 0 })
            remoteProxy.terminated(TerminatedEventArguments())
        }
        state = NotDebugging(castArgs = if (restart) state.castArgs else null)
    }

    fun print(value: String, category: String = OutputEventArgumentsCategory.STDOUT) {
        remoteProxy.output(OutputEventArguments().also {
            it.category = category
            it.output = value
        })
    }

    private fun messageWrapper(consumer: MessageConsumer) = MessageConsumer { message ->
        HexDebug.LOGGER.debug(message)
        consumer.consume(message)
    }

    private fun exceptionHandler(e: Throwable): ResponseError {
        val rootCause = ExceptionUtils.getRootCause(e) as? InvalidStateError
            ?: return RemoteEndpoint.DEFAULT_EXCEPTION_HANDLER.apply(e)

        val message = "Received message while in unexpected state: ${rootCause.state}"
        HexDebug.LOGGER.error(message)

        remoteProxy.output(OutputEventArguments().apply {
            category = "important"
            output = message
        })

        return ResponseError(ResponseErrorCode.UnknownErrorCode, message, e.stackTraceToString())
    }

    private fun setArgs(action: NotDebugging.() -> Unit) = setArgs(state.assert(), action)

    private fun setArgs(notDebugging: NotDebugging, action: NotDebugging.() -> Unit) {
        action.invoke(notDebugging)

        val debugging = notDebugging.run {
            Debugging(
                initArgs ?: return,
                launchArgs ?: return,
                castArgs ?: return,
            )
        }

        state = debugging
        remoteProxy.initialized()
    }

    private fun getDebugger() = state.assert<Debugging>().debugger

    private fun handleDebuggerStep(result: DebugStepResult?) {
        if (result == null) {
            HexDebug.LOGGER.info("Program exited, stopping debug adapter")
            terminate()
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
        setArgs { initArgs = args }
        return Capabilities().apply {
            supportsConfigurationDoneRequest = true
            supportsLoadedSourcesRequest = true
            supportsTerminateRequest = true
        }.toFuture()
    }

    override fun attach(args: MutableMap<String, Any>): CompletableFuture<Void> {
        setArgs { launchArgs = LaunchArgs(args) }
        player.displayClientMessage(Component.translatable("text.hexdebug.connected"), true)
        return futureOf()
    }

    // configuration

    override fun setBreakpoints(args: SetBreakpointsArguments): CompletableFuture<SetBreakpointsResponse> {
        return SetBreakpointsResponse().apply {
            breakpoints = getDebugger().setBreakpoints(args.source.sourceReference, args.breakpoints).toTypedArray()
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
        handleDebuggerStep(getDebugger().start())
        return futureOf()
    }

    // stepping

    override fun next(args: NextArguments?): CompletableFuture<Void> {
        handleDebuggerStep(getDebugger().executeUntilStopped(RequestStepType.OVER))
        return futureOf()
    }

    override fun continue_(args: ContinueArguments): CompletableFuture<ContinueResponse> {
        handleDebuggerStep(getDebugger().executeUntilStopped())
        return futureOf()
    }

    override fun stepIn(args: StepInArguments?): CompletableFuture<Void> {
        handleDebuggerStep(getDebugger().executeOnce())
        return futureOf()
    }

    override fun stepOut(args: StepOutArguments?): CompletableFuture<Void> {
        handleDebuggerStep(getDebugger().executeUntilStopped(RequestStepType.OUT))
        return futureOf()
    }

    override fun pause(args: PauseArguments): CompletableFuture<Void> {
        // TODO: wisps/circles?
        return futureOf()
    }

    override fun terminate(args: TerminateArguments?): CompletableFuture<Void> {
        terminate(restart = args?.restart ?: false)
        return futureOf()
    }

    override fun disconnect(args: DisconnectArguments): CompletableFuture<Void> {
        terminate(notifyClient = false, restart = args.restart)
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
            scopes = getDebugger().getScopes(args.frameId).toTypedArray()
        }.toFuture()
    }

    override fun variables(args: VariablesArguments): CompletableFuture<VariablesResponse> {
        return VariablesResponse().apply {
            variables = getDebugger().getVariables(args.variablesReference).paginate(args.start, args.count)
        }.toFuture()
    }

    override fun stackTrace(args: StackTraceArguments): CompletableFuture<StackTraceResponse> {
        return StackTraceResponse().apply {
            stackFrames = getDebugger().getStackFrames().paginate(args.startFrame, args.levels)
        }.toFuture()
    }

    override fun source(args: SourceArguments): CompletableFuture<SourceResponse> {
        return SourceResponse().apply {
            content = getDebugger().getSourceContents(args.source.sourceReference)
        }.toFuture()
    }

    override fun loadedSources(args: LoadedSourcesArguments?): CompletableFuture<LoadedSourcesResponse> {
        // apparently the client is allowed to send this request before init???
        return LoadedSourcesResponse().apply {
            sources = when (val state = state) {
                is Debugging -> state.debugger.getSources().toTypedArray()
                else -> arrayOf()
            }
        }.toFuture()
    }
}
