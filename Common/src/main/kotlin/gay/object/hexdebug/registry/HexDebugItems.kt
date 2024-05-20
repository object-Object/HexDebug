package gay.`object`.hexdebug.registry

import dev.architectury.platform.Platform
import dev.architectury.registry.item.ItemPropertiesRegistry
import gay.`object`.hexdebug.items.ItemDebugger
import gay.`object`.hexdebug.items.ItemEvaluator
import net.fabricmc.api.EnvType
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.Rarity

object HexDebugItems : HexDebugRegistrar<Item>(Registry.ITEM_REGISTRY, { Registry.ITEM }) {
    @JvmField
    val DEBUGGER = register("debugger") { ItemDebugger(unstackable) }

    @JvmField
    val EVALUATOR = register("evaluator") { ItemEvaluator(unstackable.rarity(Rarity.UNCOMMON)) }

    private val props get() = Properties().tab(HexDebugCreativeTabs.HEX_DEBUG)

    private val unstackable get() = props.stacksTo(1)

    override fun init(registerer: (ResourceLocation, Item) -> Unit) {
        super.init(registerer)
        // god i hate forge
        if (Platform.getEnv() == EnvType.CLIENT) {
            registerItemProperties()
        }
        HexDebugActions.init()
    }

    private fun registerItemProperties() {
        // TODO: add more OOP brainrot
        for ((id, function) in ItemDebugger.getProperties(DEBUGGER.value)) {
            ItemPropertiesRegistry.register(DEBUGGER.value, id, function)
        }
        for ((id, function) in ItemEvaluator.getProperties()) {
            ItemPropertiesRegistry.register(EVALUATOR.value, id, function)
        }
    }
}
