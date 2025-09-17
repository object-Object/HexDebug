package gay.`object`.hexdebug.forge.interop

import dev.emi.emi.api.EmiEntrypoint
import dev.emi.emi.api.EmiPlugin
import dev.emi.emi.api.EmiRegistry
import gay.`object`.hexdebug.interop.HexDebugEmiPlugin

@EmiEntrypoint
object ForgeHexDebugEmiPlugin : EmiPlugin {
    override fun register(registry: EmiRegistry) {
        HexDebugEmiPlugin.register(registry)
    }
}
