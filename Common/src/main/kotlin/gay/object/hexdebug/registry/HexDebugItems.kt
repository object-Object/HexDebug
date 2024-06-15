package gay.`object`.hexdebug.registry

import dev.architectury.registry.item.ItemPropertiesRegistry
import gay.`object`.hexdebug.items.DebuggerItem
import gay.`object`.hexdebug.items.EvaluatorItem
import gay.`object`.hexdebug.items.base.ItemPredicateProvider
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.Rarity

object HexDebugItems : HexDebugRegistrar<Item>(Registries.ITEM, { BuiltInRegistries.ITEM }) {
    @JvmField
    val DEBUGGER = register("debugger") { DebuggerItem(unstackable.noTab()) }

    @JvmField
    val EVALUATOR = register("evaluator") { EvaluatorItem(unstackable.rarity(Rarity.UNCOMMON)) }

    val props: Properties get() = Properties().`arch$tab`(HexDebugCreativeTabs.HEX_DEBUG.key)

    private val unstackable get() = props.stacksTo(1)

    private fun Properties.noTab() = this.`arch$tab`(null as CreativeModeTab?)

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
