package gay.`object`.hexdebug.fabric

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import gay.`object`.hexdebug.config.HexDebugConfig
import net.fabricmc.api.EnvType.CLIENT
import net.fabricmc.api.Environment

@Environment(CLIENT)
object FabricHexDebugModMenu : ModMenuApi {
    override fun getModConfigScreenFactory() = ConfigScreenFactory(HexDebugConfig::getConfigScreen)
}
