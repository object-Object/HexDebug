package gay.`object`.hexdebug.registry

import dev.architectury.registry.CreativeTabRegistry
import gay.`object`.hexdebug.HexDebug
import net.minecraft.world.item.CreativeModeTab

object HexDebugCreativeTabs {
    val HEX_DEBUG: CreativeModeTab = CreativeTabRegistry.create(HexDebug.id("hexdebug")) {
        HexDebugItems.DEBUGGER.value.noIconsInstance
    }
}
