package ca.objectobject.hexdebug

import ca.objectobject.hexdebug.registry.HexDebugItemRegistry
import ca.objectobject.hexdebug.server.HexDebugServerManager
import net.minecraft.resources.ResourceLocation
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object HexDebug {
    const val MODID = "hexdebug"

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(MODID)

    @JvmStatic
    fun init() {
        LOGGER.info("HexDebug is here!")
        HexDebugItemRegistry.init()
        HexDebugAbstractions.get().apply {
            initPlatformSpecific()
            onServerStarted {
                HexDebugServerManager.start()
            }
            onServerStopping {
                HexDebugServerManager.stop()
            }
        }
    }

    @JvmStatic
    fun id(path: String) = ResourceLocation(MODID, path)
}
