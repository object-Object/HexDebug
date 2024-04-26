package gay.`object`.hexdebug

import gay.`object`.hexdebug.networking.HexDebugNetworking
import gay.`object`.hexdebug.registry.HexDebugItems
import net.minecraft.resources.ResourceLocation
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object HexDebug {
    const val MODID = "hexdebug"

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(MODID)

    fun init() {
        LOGGER.info("HexDebug <3 HexBug")
        HexDebugItems.init()
        HexDebugNetworking.init()
    }

    @JvmStatic
    fun id(path: String) = ResourceLocation(MODID, path)
}
