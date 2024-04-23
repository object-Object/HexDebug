package gay.`object`.hexdebug

import gay.`object`.hexdebug.proxy.DebugAdapterProxyClient

object HexDebugClient {
    @JvmStatic
    fun init() {
        HexDebugAbstractions.get().apply {
            onClientJoin {
                DebugAdapterProxyClient.start()
            }
            onClientDisconnect {
                DebugAdapterProxyClient.stop()
            }
        }
    }
}
