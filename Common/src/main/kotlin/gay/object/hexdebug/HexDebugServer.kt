package gay.`object`.hexdebug

import dev.architectury.event.events.common.LifecycleEvent
import gay.`object`.hexdebug.adapter.DebugAdapterManager

object HexDebugServer {
    fun init() {
        LifecycleEvent.SERVER_STOPPING.register {
            DebugAdapterManager.stopAll()
        }
    }
}
