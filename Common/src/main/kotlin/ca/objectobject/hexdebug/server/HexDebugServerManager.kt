package ca.objectobject.hexdebug.server

import ca.objectobject.hexdebug.HexDebug
import ca.objectobject.hexdebug.debugger.DebugCastArgs
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.CancellationException
import kotlin.concurrent.thread

object HexDebugServerManager {
    var server: HexDebugServer? = null

    var queuedCast: DebugCastArgs? = null

    private var activeThread: Thread? = null
    private var serverSocket: ServerSocket? = null
    private var stop: Boolean = false

    private val port get() = 4444 // TODO: config

    fun start() {
        if (activeThread != null) {
            HexDebug.LOGGER.warn("Tried to start server manager while already running")
            return
        }

        activeThread = thread(name="HexDebugServer_$port") {
            serverSocket = ServerSocket(port)

            while (!stop) {
                HexDebug.LOGGER.info("Listening on port ${serverSocket?.localPort}...")
                val clientSocket = try {
                    serverSocket?.accept() ?: break
                } catch (_: SocketException) {
                    break
                }
                HexDebug.LOGGER.info("Client connected!")

                try {
                    server = HexDebugServer(clientSocket, queuedCast)
                    queuedCast = null
                    server?.start()?.get() // blocking
                    clientSocket.close()
                } catch (_: SocketException) {} catch (_: CancellationException) {}
                finally {
                    server = null
                }
            }

            serverSocket?.close()
        }
    }

    fun stop() {
        stop = true
        HexDebug.LOGGER.info("Stopping server manager")
        server?.stop()
        serverSocket?.close()
        activeThread?.join()
        server = null
        stop = false
    }
}