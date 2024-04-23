package gay.`object`.hexdebug.proxy

import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.networking.HexDebugNetworking
import gay.`object`.hexdebug.networking.MsgDebugAdapterProxyC2S
import org.eclipse.lsp4j.jsonrpc.JsonRpcException
import org.eclipse.lsp4j.jsonrpc.json.ConcurrentMessageProcessor
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageConsumer
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.charset.StandardCharsets
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import kotlin.concurrent.thread

data class DebugAdapterProxyClient(val input: InputStream, val output: OutputStream) {
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

        private var activeThread: Thread? = null
        private var serverSocket: ServerSocket? = null
        private var stop: Boolean = false

        private val port get() = 4444 // TODO: config

        private val executorService = Executors.newCachedThreadPool()

        fun start() {
            if (activeThread != null) {
                HexDebug.LOGGER.warn("Tried to start DebugAdapterProxyClient while already running")
                return
            }

            activeThread = thread(name="DebugAdapterProxyClient_$port") {
                serverSocket = ServerSocket(port)
                while (!stop) {
                    HexDebug.LOGGER.info("Listening on port ${serverSocket?.localPort}...")
                    val clientSocket = try {
                        serverSocket?.accept() ?: break
                    } catch (_: SocketException) {
                        break
                    }
                    HexDebug.LOGGER.info("Client connected!")

                    instance = DebugAdapterProxyClient(clientSocket)

                    try {
                        instance?.processor?.beginProcessing(executorService)?.get()
                    } catch (_: SocketException) {
                    } catch (_: CancellationException) {
                    } finally {
                        instance = null
                    }
                }
                serverSocket?.close()
            }
        }

        fun stop() {
            stop = true
            HexDebug.LOGGER.info("Stopping DebugAdapterProxyClient")
            serverSocket?.close()
            activeThread?.join()
            instance = null
            stop = false
        }
    }
}
