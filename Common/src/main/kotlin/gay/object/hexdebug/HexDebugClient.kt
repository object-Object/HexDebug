package gay.`object`.hexdebug

import dev.architectury.event.events.client.ClientPlayerEvent
import gay.`object`.hexdebug.adapter.proxy.DebugAdapterProxyClient

object HexDebugClient {
    fun init() {
        DebugAdapterProxyClient.init()
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register {
            DebugAdapterProxyClient.start()
        }
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register {
            DebugAdapterProxyClient.stop()
        }
    }
}
