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
import net.minecraft.world.level.ItemLike

object HexDebugItems : HexDebugRegistrar<Item>(Registries.ITEM, { BuiltInRegistries.ITEM }) {
    @JvmField
    val DEBUGGER = item("debugger") {
        DebuggerItem(unstackable.rarity(Rarity.UNCOMMON).noTab(), isQuenched = false)
    }

    @JvmField
    val QUENCHED_DEBUGGER = item("quenched_debugger") {
        DebuggerItem(unstackable.rarity(Rarity.RARE).noTab(), isQuenched = true)
    }

    @JvmField
    val EVALUATOR = item("evaluator") {
        EvaluatorItem(unstackable.rarity(Rarity.UNCOMMON), isQuenched = false)
    }

    @JvmField
    val QUENCHED_EVALUATOR = item("quenched_evaluator") {
        EvaluatorItem(unstackable.rarity(Rarity.RARE), isQuenched = true)
    }

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

    private fun <V : Item> item(name: String, builder: () -> V) = ItemEntry(register(name, builder))

    class ItemEntry<V : Item>(entry: Entry<V>) : Entry<V>(entry), ItemLike {
        override fun asItem() = value
    }
}
