package gay.`object`.hexdebug.registry

import dev.architectury.registry.menu.MenuRegistry
import gay.`object`.hexdebug.gui.splicing.SplicingTableMenu
import gay.`object`.hexdebug.gui.splicing.SplicingTableScreen
import net.minecraft.core.Registry
import net.minecraft.world.inventory.MenuType

object HexDebugMenus : HexDebugRegistrar<MenuType<*>>(Registry.MENU_REGISTRY, { Registry.MENU }) {
    @JvmField
    val SPLICING_TABLE = register("splicing_table") {
        MenuType(::SplicingTableMenu)
    }

    override fun initClient() {
        MenuRegistry.registerScreenFactory(SPLICING_TABLE.value, ::SplicingTableScreen)
    }
}
