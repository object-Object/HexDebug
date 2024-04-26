package gay.`object`.hexdebug.registry

import gay.`object`.hexdebug.item.DebuggerItem
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.Item

object HexDebugItems : HexDebugRegistry<Item>(Registries.ITEM) {
    val DEBUGGER = register("debugger") { DebuggerItem(Item.Properties()) }
}
