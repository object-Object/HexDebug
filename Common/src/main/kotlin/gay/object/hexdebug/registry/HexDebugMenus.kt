package gay.`object`.hexdebug.registry

import dev.architectury.registry.menu.MenuRegistry
import gay.`object`.hexdebug.gui.focusholder.FocusHolderMenu
import gay.`object`.hexdebug.gui.focusholder.FocusHolderScreen
import gay.`object`.hexdebug.gui.splicing.SplicingTableMenu
import gay.`object`.hexdebug.gui.splicing.SplicingTableScreen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.inventory.MenuType

object HexDebugMenus : HexDebugRegistrar<MenuType<*>>(Registries.MENU, { BuiltInRegistries.MENU }) {
    @JvmField
    val SPLICING_TABLE = register("splicing_table") {
        MenuType(::SplicingTableMenu, FeatureFlags.DEFAULT_FLAGS)
    }

    @JvmField
    val FOCUS_HOLDER = register("focus_holder") {
        MenuType(::FocusHolderMenu, FeatureFlags.DEFAULT_FLAGS)
    }

    override fun initClient() {
        MenuRegistry.registerScreenFactory(SPLICING_TABLE.value, ::SplicingTableScreen)
        MenuRegistry.registerScreenFactory(FOCUS_HOLDER.value, ::FocusHolderScreen)
    }
}
