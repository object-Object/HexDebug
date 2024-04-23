package gay.`object`.hexdebug

import gay.`object`.hexdebug.adapter.DebugAdapterManager
import gay.`object`.hexdebug.networking.HexDebugNetworking
import gay.`object`.hexdebug.registry.HexDebugItemRegistry
import net.minecraft.resources.ResourceLocation
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object HexDebug {
    const val MODID = "hexdebug"

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(MODID)

    @JvmStatic
    fun init() {
        LOGGER.info("HexDebug <3 HexBug")
        HexDebugItemRegistry.init()
        HexDebugNetworking.init()
        HexDebugAbstractions.get().apply {
            initPlatformSpecific()
            onServerStopping {
                DebugAdapterManager.stopAll()
            }
        }
    }

    @JvmStatic
    fun id(path: String) = ResourceLocation(MODID, path)
}
