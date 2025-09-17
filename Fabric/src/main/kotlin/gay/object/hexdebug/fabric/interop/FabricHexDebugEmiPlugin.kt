package gay.`object`.hexdebug.fabric.interop

import dev.emi.emi.api.EmiPlugin
import dev.emi.emi.api.EmiRegistry
import gay.`object`.hexdebug.interop.HexDebugEmiPlugin

object FabricHexDebugEmiPlugin : EmiPlugin {
    override fun register(registry: EmiRegistry) {
        HexDebugEmiPlugin.register(registry)
    }
}
