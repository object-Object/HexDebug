package gay.`object`.hexdebug.registry

import dev.architectury.registry.item.ItemPropertiesRegistry
import gay.`object`.hexdebug.items.ItemDebugger
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.Rarity

object HexDebugItems : HexDebugRegistry<Item>(Registries.ITEM) {
    val DEBUGGER by register("debugger") { ItemDebugger(unstackable.rarity(Rarity.RARE)) }

    private val props get() = Properties()
    private val unstackable get() = props.stacksTo(1)

    fun registerItemProperties() {
        // TODO: add more OOP brainrot
        for ((id, function) in ItemDebugger.getProperties()) {
            ItemPropertiesRegistry.register(DEBUGGER::get, id, function)
        }
    }
}
