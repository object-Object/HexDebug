package gay.`object`.hexdebug.registry

import dev.architectury.platform.Platform
import dev.architectury.registry.item.ItemPropertiesRegistry
import gay.`object`.hexdebug.items.DebuggerItem
import gay.`object`.hexdebug.items.EvaluatorItem
import net.fabricmc.api.EnvType
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.Rarity

object HexDebugItems : HexDebugRegistrar<Item>(Registry.ITEM_REGISTRY, { Registry.ITEM }) {
    @JvmField
    val DEBUGGER = register("debugger") { DebuggerItem(unstackable) }

    @JvmField
    val EVALUATOR = register("evaluator") { EvaluatorItem(unstackable.rarity(Rarity.UNCOMMON)) }

    private val props: Properties get() = Properties().tab(HexDebugCreativeTabs.HEX_DEBUG)

    private val unstackable get() = props.stacksTo(1)

    override fun init(registerer: (ResourceLocation, Item) -> Unit) {
        super.init(registerer)
        HexDebugActions.init() // TODO: ???????????????????????
    }

    override fun initClient() {
        registerItemProperties()
    }

    private fun registerItemProperties() {
        for (entry in entries) {
            (entry.value as? ItemPredicateProvider)?.getModelPredicates()?.forEach {
                ItemPropertiesRegistry.register(entry.value, it.id, it.predicate)
            }
        }
    }
}
