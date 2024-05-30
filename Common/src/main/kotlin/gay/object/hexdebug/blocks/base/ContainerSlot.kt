package gay.`object`.hexdebug.blocks.base

import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import kotlin.reflect.KProperty

data class ContainerSlot(val slot: Int) {
    operator fun getValue(container: Container, property: KProperty<*>) = container.getItem(slot)

    operator fun setValue(container: Container, property: KProperty<*>, stack: ItemStack) {
        container.setItem(slot, stack)
    }
}
