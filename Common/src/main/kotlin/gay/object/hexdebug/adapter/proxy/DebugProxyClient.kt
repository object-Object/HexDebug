package gay.`object`.hexdebug.adapter.proxy

import dev.architectury.event.events.client.ClientPlayerEvent
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.config.HexDebugConfig
import gay.`object`.hexdebug.items.ItemDebugger
import gay.`object`.hexdebug.networking.msg.MsgDebugAdapterProxy
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*
import net.minecraft.world.InteractionResult
import org.eclipse.lsp4j.jsonrpc.JsonRpcException
import org.eclipse.lsp4j.jsonrpc.json.ConcurrentMessageProcessor
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageConsumer
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import io.ktor.network.sockets.Socket as KtorSocket

data class DebugProxyClient(val input: InputStream, val output: OutputStream) {
    constructor(socket: KtorSocket) : this(
        socket.openReadChannel().toInputStream(),
        socket.openWriteChannel().toOutputStream(),
    )

    private val proxyProducer = DebugProxyClientProducer(input)
    private val proxyConsumer = DebugProxyClientConsumer(output)

    // no need to pass consumer here, we're not using the consume(Message) method on this side
    private val processor = ConcurrentMessageProcessor(proxyProducer, null)

    fun consume(content: String) = proxyConsumer.consume(content)

    companion object {
        var instance: DebugProxyClient? = null
            private set

        private val enabled get() = HexDebugConfig.get().client.openDebugPort
        private val port get() = HexDebugConfig.get().client.debugPort

        private val executorService = Executors.newCachedThreadPool()

        private var thread: Thread? = null
        private var wrapperJob: Job? = null
        private var serverJob: Job? = null

        fun init() {
            HexDebugConfig.getHolder().registerSaveListener { _, _ ->
                reload()
                InteractionResult.PASS
            }
            ClientPlayerEvent.CLIENT_PLAYER_JOIN.register {
                ItemDebugger.debugState = ItemDebugger.DebugState.NOT_DEBUGGING
                start()
            }
            ClientPlayerEvent.CLIENT_PLAYER_QUIT.register {
                stop()
            }
        }

        private fun start() {
            thread = thread?.also {
                HexDebug.LOGGER.warn("Tried to start DebugAdapterProxyClient while already running")
            } ?: thread(name="DebugAdapterProxyClient_$port") {
                runBlocking {
                    wrapperJob = launch { runServerWrapper() }
                }
                thread = null
            }
        }

        private fun reload() {
            serverJob?.cancel()
        }

        private fun stop() {
            if (thread == null) return
            HexDebug.LOGGER.debug("Stopping DebugAdapterProxyClient")
            wrapperJob?.cancel()
            thread?.join()
            HexDebug.LOGGER.debug("Stopped DebugAdapterProxyClient")
        }

        private suspend fun runServerWrapper() {
            val selector = SelectorManager(Dispatchers.IO)
            while (true) {
                coroutineScope {
                    serverJob = launch { runServer(selector) }
                }
            }
        }

        private suspend fun runServer(selector: SelectorManager) {
            if (!enabled) {
                awaitCancellation()
            }
            HexDebug.LOGGER.info("Listening for debug client on port {}...", port)
            aSocket(selector).tcp().bind(port = port).use { serverSocket ->
                while (true) {
                    acceptClient(serverSocket)
                }
            }
        }

        private suspend fun acceptClient(serverSocket: ServerSocket) {
            HexDebug.LOGGER.debug("Waiting for client...")
            serverSocket.accept().use { clientSocket ->
                HexDebug.LOGGER.debug("Debug client connected!")
                try {
                    instance = DebugProxyClient(clientSocket)
                    runInterruptible(Dispatchers.IO) {
                        instance?.processor?.beginProcessing(executorService)?.get()
                    }
                } finally {
                    instance = null
                }
            }
        }
    }
}

class DebugProxyClientProducer(input: InputStream) : StreamMessageProducer(input, null) {
    override fun handleMessage(input: InputStream, headers: Headers): Boolean {
        try {
            val contentLength = headers.contentLength
            val buffer = ByteArray(contentLength)
            var bytesRead = 0

            while (bytesRead < contentLength) {
                val readResult = input.read(buffer, bytesRead, contentLength - bytesRead)
                if (readResult == -1) return false
                bytesRead += readResult
            }

            // instead of parsing the message here, just forward it to the server
            val content = String(buffer, charset(headers.charset))
            MsgDebugAdapterProxy(content).sendToServer()
        } catch (exception: Exception) {
            // UnsupportedEncodingException can be thrown by String constructor
            // JsonParseException can be thrown by jsonHandler
            // We also catch arbitrary exceptions that are thrown by message consumers in order to keep this thread alive
            fireError(exception)
        }
        return true
    }
}

class DebugProxyClientConsumer(output: OutputStream) : StreamMessageConsumer(output, null) {
    private val encoding = StandardCharsets.UTF_8.name()
    private val outputLock = Any()

    fun consume(content: String) {
        try {
            val contentBytes = content.toByteArray(charset(encoding))
            val contentLength = contentBytes.size

            val header = getHeader(contentLength)
            val headerBytes = header.toByteArray(StandardCharsets.US_ASCII)

            synchronized(outputLock) {
                output.write(headerBytes)
                output.write(contentBytes)
                output.flush()
            }
        } catch (exception: IOException) {
            throw JsonRpcException(exception)
        }
    }
}
