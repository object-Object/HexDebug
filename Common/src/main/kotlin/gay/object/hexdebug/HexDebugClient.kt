package gay.`object`.hexdebug

import gay.`object`.hexdebug.adapter.proxy.DebugProxyClient
import gay.`object`.hexdebug.config.HexDebugConfig
import gay.`object`.hexdebug.config.HexDebugConfig.GlobalConfig
import me.shedaniel.autoconfig.AutoConfig
import net.minecraft.client.gui.screens.Screen

object HexDebugClient {
    fun init() {
        HexDebugConfig.initClient()
        DebugProxyClient.init()
    }

    fun getConfigScreen(parent: Screen): Screen {
        return AutoConfig.getConfigScreen(GlobalConfig::class.java, parent).get()
    }
}
