package gay.`object`.hexdebug

import gay.`object`.hexdebug.adapter.DebugAdapterManager
import gay.`object`.hexdebug.config.HexDebugConfig
import gay.`object`.hexdebug.networking.HexDebugNetworking
import gay.`object`.hexdebug.registry.*
import net.minecraft.resources.ResourceLocation
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object HexDebug {
    const val MODID = "hexdebug"

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(MODID)

    fun init() {
        LOGGER.info("HexDebug <3 HexBug")
        HexDebugConfig.init()
        initRegistries(
            HexDebugBlocks,
            HexDebugBlockEntities,
            HexDebugItems,
            HexDebugCreativeTabs,
            HexDebugMenus,
            HexDebugActions,
            HexDebugContinuationTypes,
            HexDebugIotaTypes,
        )
        HexDebugNetworking.init()
        DebugAdapterManager.init()
    }

    @JvmStatic
    fun id(path: String) = ResourceLocation(MODID, path)
}
