package gay.`object`.hexdebug.config

import at.petrak.hexcasting.api.misc.MediaConstants
import gay.`object`.hexdebug.HexDebug
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry.Category
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Tooltip
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.TransitiveObject
import me.shedaniel.autoconfig.serializer.PartitioningSerializer
import me.shedaniel.autoconfig.serializer.PartitioningSerializer.GlobalData
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer
import net.minecraft.client.gui.screens.Screen

// we can't use a companion object because GlobalData sees the field and throws an error :/

object HexDebugConfig {
    fun init() {
        AutoConfig.register(
            GlobalConfig::class.java,
            PartitioningSerializer.wrap(::Toml4jConfigSerializer),
        )
    }

    fun getConfigScreen(parent: Screen): Screen = AutoConfig.getConfigScreen(GlobalConfig::class.java, parent).get()

    // functions instead of getters to make it more clear that these can't be used until after init()
    fun getHolder() = AutoConfig.getConfigHolder(GlobalConfig::class.java)!!

    fun get() = getHolder().config!!

    @Config(name = HexDebug.MODID)
    class GlobalConfig : GlobalData() {
        @Category("client")
        @TransitiveObject
        val client = ClientConfig()

        @Category("server")
        @TransitiveObject
        val server = ServerConfig()
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
    }

    @Config(name = "server")
    class ServerConfig : ConfigData {
        @Tooltip
        val maxUndoStackSize: Int = 64

        @Tooltip
        val splicingTableMediaCost: Long = MediaConstants.DUST_UNIT / 20

        @Tooltip
        val splicingTableMaxMedia: Long = MediaConstants.CRYSTAL_UNIT
    }
}

enum class DebuggerDisplayMode {
    DISABLED,
    NOT_CONNECTED,
    ENABLED,
}
