package gay.`object`.hexdebug.adapter.proxy

import gay.`object`.hexdebug.adapter.DebugAdapter
import gay.`object`.hexdebug.networking.HexDebugNetworking
import gay.`object`.hexdebug.networking.MsgDebugAdapterProxyS2C
import net.minecraft.server.level.ServerPlayer
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import org.eclipse.lsp4j.jsonrpc.*
import org.eclipse.lsp4j.jsonrpc.debug.DebugLauncher
import org.eclipse.lsp4j.jsonrpc.debug.DebugRemoteEndpoint
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageConsumer
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer
import org.eclipse.lsp4j.jsonrpc.messages.Message
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints
import java.io.IOException
import java.util.concurrent.Future

data class DebugAdapterProxyServer(val player: ServerPlayer, val debugAdapter: DebugAdapter) {
    private val launcher = ProxyBuilder()
        .setLocalService(debugAdapter)
        .setRemoteInterface(IDebugProtocolClient::class.java)
        .create() as ProxyLauncher

    init {
        debugAdapter.launcher = launcher
    }

    inner class ProxyProducer(
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

    fun produce(content: String) = launcher.proxyProducer.produce(content)

    inner class ProxyConsumer(private val jsonHandler: MessageJsonHandler) : StreamMessageConsumer(null, jsonHandler) {
        override fun consume(message: Message) {
            try {
                val content = jsonHandler.serialize(message)
                HexDebugNetworking.sendToPlayer(player, MsgDebugAdapterProxyS2C(content))
            } catch (exception: IOException) {
                throw JsonRpcException(exception)
            }
        }
    }

    inner class ProxyLauncher(
        val proxyProducer: ProxyProducer,
        private val remoteEndpoint: RemoteEndpoint,
        private val remoteProxy: IDebugProtocolClient,
    ) : Launcher<IDebugProtocolClient> {
        override fun startListening(): Future<Void> {
            TODO("Not yet implemented")
        }

        override fun getRemoteEndpoint() = remoteEndpoint

        override fun getRemoteProxy() = remoteProxy
    }

    inner class ProxyBuilder : DebugLauncher.Builder<IDebugProtocolClient>() {
        override fun create(): ProxyLauncher {
            // Validate input
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
            val proxyProducer = ProxyProducer(messageConsumer, jsonHandler, remoteEndpoint)
            return ProxyLauncher(proxyProducer, remoteEndpoint, remoteProxy)
        }

        override fun createRemoteEndpoint(jsonHandler: MessageJsonHandler): RemoteEndpoint {
            val outgoingMessageStream = wrapMessageConsumer(ProxyConsumer(jsonHandler))
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
