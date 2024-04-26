package gay.`object`.hexdebug.config

import gay.`object`.hexdebug.HexDebug
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry.Category
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.TransitiveObject
import me.shedaniel.autoconfig.serializer.PartitioningSerializer
import me.shedaniel.autoconfig.serializer.PartitioningSerializer.GlobalData
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer

// we can't use a companion object because GlobalData sees the field and throws an error :/

object HexDebugConfig {
    fun init() {
        AutoConfig.register(
            Global::class.java,
            PartitioningSerializer.wrap(::Toml4jConfigSerializer),
        )
    }

    // functions instead of getters to make it more clear that these can't be used until after init()
    fun getHolder() = AutoConfig.getConfigHolder(Global::class.java)!!

    fun get() = getHolder().config!!

    @Config(name = HexDebug.MODID)
    class Global : GlobalData() {
        @Category("client")
        @TransitiveObject
        val client = Client()
    }

    @Config(name = "client")
    class Client : ConfigData {
        val debugPort: Int = 4444
    }
}
