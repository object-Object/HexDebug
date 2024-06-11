package gay.`object`.hexdebug.fabric

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import gay.`object`.hexdebug.config.HexDebugConfig

object FabricHexDebugModMenu : ModMenuApi {
    override fun getModConfigScreenFactory() = ConfigScreenFactory(HexDebugConfig::getConfigScreen)
}
