package gay.`object`.hexdebug.config

import gay.`object`.hexdebug.HexDebug
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.ConfigHolder
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry.Category
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Tooltip
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.TransitiveObject
import me.shedaniel.autoconfig.serializer.PartitioningSerializer
import me.shedaniel.autoconfig.serializer.PartitioningSerializer.GlobalData
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer
import net.minecraft.world.InteractionResult

object HexDebugClientConfig {
    @JvmStatic
    lateinit var holder: ConfigHolder<GlobalConfig>

    @JvmStatic
    val config get() = holder.config.client

    fun init() {
        holder = AutoConfig.register(
            GlobalConfig::class.java,
            PartitioningSerializer.wrap(::Toml4jConfigSerializer),
        )

        // when we change the server config in the client gui, also send it to the server config class
        holder.registerSaveListener { _, config ->
            HexDebugServerConfig.holder.config = HexDebugServerConfig.GlobalConfig(config.server)
            InteractionResult.PASS
        }
    }

    @Config(name = HexDebug.MODID)
    class GlobalConfig : GlobalData() {
        @Category("client")
        @TransitiveObject
        val client = ClientConfig()

        @Category("server")
        @TransitiveObject
        val server = HexDebugServerConfig.ServerConfig()
    }

    @Config(name = "client")
    class ClientConfig : ConfigData {
        @Tooltip
        val openDebugPort: Boolean = true

        @Tooltip
        val debugPort: Int = 4444

        @Tooltip
        val smartDebuggerSneakScroll: Boolean = true

        @Tooltip
        val debuggerDisplayMode: DebuggerDisplayMode = DebuggerDisplayMode.ENABLED

        @Tooltip
        val showDebugClientLineNumber: Boolean = false

        @Tooltip
        val invertSplicingTableScrollDirection: Boolean = false
    }
}

enum class DebuggerDisplayMode {
    DISABLED,
    NOT_CONNECTED,
    ENABLED,
}