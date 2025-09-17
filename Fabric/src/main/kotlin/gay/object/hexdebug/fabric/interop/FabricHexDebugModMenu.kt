package gay.`object`.hexdebug.fabric.interop

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import gay.`object`.hexdebug.HexDebugClient

object FabricHexDebugModMenu : ModMenuApi {
    override fun getModConfigScreenFactory() = ConfigScreenFactory(HexDebugClient::getConfigScreen)
}
