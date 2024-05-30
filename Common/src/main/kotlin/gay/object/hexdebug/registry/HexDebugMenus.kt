package gay.`object`.hexdebug.registry

import dev.architectury.registry.menu.MenuRegistry
import gay.`object`.hexdebug.gui.SplicingTableMenu
import gay.`object`.hexdebug.gui.SplicingTableScreen
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.inventory.MenuType

object HexDebugMenus : HexDebugRegistrar<MenuType<*>>(Registries.MENU, { BuiltInRegistries.MENU }) {
    @JvmField
    val SPLICING_TABLE = register("splicing_table") { MenuType(::SplicingTableMenu, FeatureFlags.DEFAULT_FLAGS) }

    @Environment(EnvType.CLIENT)
    override fun initClient() {
        MenuRegistry.registerScreenFactory(SPLICING_TABLE.value, ::SplicingTableScreen)
    }
}
