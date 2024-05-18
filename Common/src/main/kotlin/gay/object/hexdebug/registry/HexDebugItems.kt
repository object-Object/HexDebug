package gay.`object`.hexdebug.registry

import dev.architectury.platform.Platform
import dev.architectury.registry.item.ItemPropertiesRegistry
import gay.`object`.hexdebug.items.ItemDebugger
import net.fabricmc.api.EnvType
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties

object HexDebugItems : HexDebugRegistrar<Item>(Registry.ITEM_REGISTRY, { Registry.ITEM }) {
    @JvmField
    val DEBUGGER = register("debugger") {
        ItemDebugger(unstackable.tab(HexDebugCreativeTabs.HEX_DEBUG))
    }

    // TODO: maybe we should have our own tab, but I'm gonna be lazy for now
    private val props get() = Properties()

    private val unstackable get() = props.stacksTo(1)

    override fun init(registerer: (ResourceLocation, Item) -> Unit) {
        super.init(registerer)
        // god i hate forge
        if (Platform.getEnv() == EnvType.CLIENT) {
            registerItemProperties()
        }
    }

    private fun registerItemProperties() {
        // TODO: add more OOP brainrot
        for ((id, function) in ItemDebugger.getProperties(DEBUGGER.value)) {
            ItemPropertiesRegistry.register(DEBUGGER.value, id, function)
        }
    }
}
