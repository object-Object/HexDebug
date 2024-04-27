package gay.`object`.hexdebug

import gay.`object`.hexdebug.adapter.proxy.DebugProxyClient
import gay.`object`.hexdebug.registry.HexDebugItems

object HexDebugClient {
    fun init() {
        DebugProxyClient.init()
        HexDebugItems.registerItemProperties()
    }
}
