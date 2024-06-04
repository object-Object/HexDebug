package gay.`object`.hexdebug.blocks.base

import net.minecraft.world.inventory.ContainerData
import kotlin.reflect.KProperty

data class ContainerDataDelegate(val data: ContainerData, val index: Int) {
    operator fun getValue(thisRef: Any, property: KProperty<*>) = data.get(index)

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: Int) {
        data.set(index, value)
    }
}
