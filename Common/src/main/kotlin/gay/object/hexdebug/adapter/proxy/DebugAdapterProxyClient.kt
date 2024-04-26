package gay.`object`.hexdebug.adapter.proxy

import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.config.HexDebugConfig
import gay.`object`.hexdebug.networking.HexDebugNetworking
import gay.`object`.hexdebug.networking.MsgDebugAdapterProxyC2S
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*
import org.eclipse.lsp4j.jsonrpc.JsonRpcException
import org.eclipse.lsp4j.jsonrpc.json.ConcurrentMessageProcessor
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageConsumer
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import io.ktor.network.sockets.Socket as KtorSocket

data class DebugAdapterProxyClient(val input: InputStream, val output: OutputStream) {
    constructor(socket: KtorSocket) : this(
        socket.openReadChannel().toInputStream(),
        socket.openWriteChannel().toOutputStream(),
    )

    constructor(socket: Socket) : this(socket.inputStream, socket.outputStream)

    private val proxyProducer = ProxyProducer()
    private val proxyConsumer = ProxyConsumer()
    // no need to pass consumer here, we're not using the consume(Message) method on this side
    private val processor = ConcurrentMessageProcessor(proxyProducer, null)

    fun consume(content: String) = proxyConsumer.consume(content)

    inner class ProxyProducer : StreamMessageProducer(input, null) {
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
                HexDebugNetworking.sendToServer(MsgDebugAdapterProxyC2S(content))
            } catch (exception: Exception) {
                // UnsupportedEncodingException can be thrown by String constructor
                // JsonParseException can be thrown by jsonHandler
                // We also catch arbitrary exceptions that are thrown by message consumers in order to keep this thread alive
                fireError(exception)
            }
            return true
        }
    }

    inner class ProxyConsumer : StreamMessageConsumer(output, null) {
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

    companion object {
        var instance: DebugAdapterProxyClient? = null
            private set

        private val port get() = HexDebugConfig.getClient().debugPort

        private val executorService = Executors.newCachedThreadPool()

        private var thread: Thread? = null
        private var job: Job? = null

        fun start() {
            thread = thread?.also {
                HexDebug.LOGGER.warn("Tried to start DebugAdapterProxyClient while already running")
            } ?: thread(name="DebugAdapterProxyClient_$port") {
                runBlocking {
                    job = launch {
                        runServer()
                    }
                }
                thread = null
            }
        }

        fun stop() {
            HexDebug.LOGGER.info("Stopping DebugAdapterProxyClient")
            runBlocking {
                job?.cancel()
            }
            thread?.join()
            HexDebug.LOGGER.info("Stopped DebugAdapterProxyClient")
        }

        private suspend fun runServer() {
            val selector = SelectorManager(Dispatchers.IO)
            aSocket(selector).tcp().bind(port = port).use { serverSocket ->
                while (true) {
                    acceptClient(serverSocket)
                }
            }
        }

        private suspend fun acceptClient(serverSocket: ServerSocket) {
            HexDebug.LOGGER.info("Listening for debug client on port {}...", port)
            serverSocket.accept().use { clientSocket ->
                HexDebug.LOGGER.info("Debug client connected!")

                try {
                    instance = DebugAdapterProxyClient(clientSocket)
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
