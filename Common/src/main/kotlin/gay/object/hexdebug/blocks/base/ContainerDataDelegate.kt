package gay.`object`.hexdebug.blocks.base

import net.minecraft.world.inventory.ContainerData
import kotlin.reflect.KProperty

data class ContainerDataDelegate(val data: ContainerData, val index: Int) {
    operator fun getValue(thisRef: Any, property: KProperty<*>) = data.get(index)

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: Int) {
        data.set(index, value)
    }
}

data class ContainerDataLongDelegate(val data: ContainerData, val lowIndex: Int, val highIndex: Int) {
    operator fun getValue(thisRef: Any, property: KProperty<*>): Long {
        val low = data.get(lowIndex).toLong()
        val high = data.get(highIndex).toLong() shl 32
        return low + high
    }

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: Long) {
        data.set(lowIndex, value.toInt())
        data.set(highIndex, (value shr 32).toInt())
    }
}
