package gay.`object`.hexdebug

import at.petrak.hexcasting.api.client.ScryingLensOverlayRegistry
import gay.`object`.hexdebug.adapter.proxy.DebugProxyClient
import gay.`object`.hexdebug.blocks.focusholder.FocusHolderBlock
import gay.`object`.hexdebug.config.HexDebugConfig
import gay.`object`.hexdebug.config.HexDebugConfig.GlobalConfig
import gay.`object`.hexdebug.registry.HexDebugBlocks
import me.shedaniel.autoconfig.AutoConfig
import net.minecraft.client.gui.screens.Screen

object HexDebugClient {
    fun init() {
        HexDebugConfig.initClient()
        DebugProxyClient.init()
        addScryingLensOverlays()
    }

    fun getConfigScreen(parent: Screen): Screen {
        return AutoConfig.getConfigScreen(GlobalConfig::class.java, parent).get()
    }

    private fun addScryingLensOverlays() {
        ScryingLensOverlayRegistry.addDisplayer(HexDebugBlocks.FOCUS_HOLDER.id) { lines, _, pos, _, level, _ ->
            FocusHolderBlock.getBlockEntity(level, pos)?.addScryingLensLines(lines)
        }
    }
}
