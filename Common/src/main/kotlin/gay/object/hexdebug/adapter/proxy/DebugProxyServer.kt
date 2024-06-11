package gay.`object`.hexdebug.adapter.proxy

import gay.`object`.hexdebug.adapter.DebugAdapter
import gay.`object`.hexdebug.adapter.IHexDebugLauncher
import gay.`object`.hexdebug.networking.msg.MsgDebugAdapterProxy
import net.minecraft.server.level.ServerPlayer
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import org.eclipse.lsp4j.jsonrpc.*
import org.eclipse.lsp4j.jsonrpc.debug.DebugLauncher
import org.eclipse.lsp4j.jsonrpc.debug.DebugRemoteEndpoint
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageConsumer
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer
import org.eclipse.lsp4j.jsonrpc.messages.Message
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints
import java.io.IOException

class DebugProxyServerLauncher(
    private val proxyProducer: DebugProxyServerProducer,
    private val remoteEndpoint: RemoteEndpoint,
    private val remoteProxy: IDebugProtocolClient,
) : IHexDebugLauncher {
    override fun handleMessage(content: String) = proxyProducer.produce(content)

    override fun startListening() = throw AssertionError()

    override fun getRemoteEndpoint() = remoteEndpoint

    override fun getRemoteProxy() = remoteProxy

    companion object {
        fun createLauncher(
            debugAdapter: DebugAdapter,
            wrapper: ((MessageConsumer) -> MessageConsumer)? = null,
            exceptionHandler: ((Throwable) -> ResponseError)? = null,
        ): DebugProxyServerLauncher {
            val builder = Builder()
                .setPlayer(debugAdapter.player)
                .setLocalService(debugAdapter)
                .setRemoteInterface(IDebugProtocolClient::class.java)
            if (wrapper != null) builder.wrapMessages(wrapper)
            if (exceptionHandler != null) builder.setExceptionHandler(exceptionHandler)
            return builder.create() as DebugProxyServerLauncher
        }
    }

    class Builder : DebugLauncher.Builder<IDebugProtocolClient>() {
        private var player: ServerPlayer? = null

        fun setPlayer(player: ServerPlayer) = this.also { it.player = player }

        override fun create(): DebugProxyServerLauncher {
            // Validate input
            checkNotNull(player) { "Player must be configured." }
            checkNotNull(localServices) { "Local service must be configured." }
            checkNotNull(remoteInterfaces) { "Remote interface must be configured." }

            // Create the JSON handler, remote endpoint and remote proxy
            val jsonHandler = createJsonHandler()
            if (messageTracer != null) {
                messageTracer.setJsonHandler(jsonHandler)
            }
            val remoteEndpoint = createRemoteEndpoint(jsonHandler)
            val remoteProxy = createProxy(remoteEndpoint)

            val messageConsumer = wrapMessageConsumer(remoteEndpoint)
            val proxyProducer = DebugProxyServerProducer(messageConsumer, jsonHandler, remoteEndpoint)
            return DebugProxyServerLauncher(proxyProducer, remoteEndpoint, remoteProxy)
        }

        override fun createRemoteEndpoint(jsonHandler: MessageJsonHandler): RemoteEndpoint {
            val outgoingMessageStream = wrapMessageConsumer(DebugProxyServerConsumer(player!!, jsonHandler))
            val localEndpoint = ServiceEndpoints.toEndpoint(localServices)
            val remoteEndpoint: RemoteEndpoint =
                if (exceptionHandler == null) DebugRemoteEndpoint(outgoingMessageStream, localEndpoint)
                else DebugRemoteEndpoint(outgoingMessageStream, localEndpoint, exceptionHandler)
            jsonHandler.methodProvider = remoteEndpoint
            remoteEndpoint.jsonHandler = jsonHandler
            return remoteEndpoint
        }
    }
}

class DebugProxyServerProducer(
    private val callback: MessageConsumer,
    private val jsonHandler: MessageJsonHandler,
    private val issueHandler: MessageIssueHandler,
) : StreamMessageProducer(null, jsonHandler, issueHandler) {
    fun produce(content: String) {
        try {
            val message = jsonHandler.parseMessage(content)
            callback.consume(message)
        } catch (exception: MessageIssueException) {
            // An issue was found while parsing or validating the message
            issueHandler.handle(exception.rpcMessage, exception.issues)
        }
    }
}

class DebugProxyServerConsumer(
    private val player: ServerPlayer,
    private val jsonHandler: MessageJsonHandler,
) : StreamMessageConsumer(null, jsonHandler) {
    override fun consume(message: Message) {
        try {
            val content = jsonHandler.serialize(message)
            MsgDebugAdapterProxy(content).sendToPlayer(player)
        } catch (exception: IOException) {
            throw JsonRpcException(exception)
        }
    }
}
