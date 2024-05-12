package gay.`object`.hexdebug.registry

import dev.architectury.registry.CreativeTabRegistry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab

object HexDebugCreativeTabs : HexDebugRegistrar<CreativeModeTab>(
    Registries.CREATIVE_MODE_TAB,
    { BuiltInRegistries.CREATIVE_MODE_TAB },
) {
    val HEX_DEBUG = make("hexdebug") {
        icon { HexDebugItems.DEBUGGER.value.noIconsInstance }
        displayItems { _, output ->
            output.accept(HexDebugItems.DEBUGGER.value.defaultInstance)
        }
    }

    @Suppress("SameParameterValue")
    private fun make(name: String, action: CreativeModeTab.Builder.() -> Unit) = register(name) {
        CreativeTabRegistry.create { builder ->
            builder.title(Component.translatable("itemGroup.$name"))
            action.invoke(builder)
        }
    }
}
