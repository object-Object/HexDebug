package gay.`object`.hexdebug.blocks.base

import net.minecraft.world.inventory.ContainerData
import kotlin.reflect.KProperty

// DataSlot stores ints but serializes them as shorts??????????????????????????????????????????????
// TODO: idk if this works for negative values but i don't need it to be negative so idc
data class ContainerDataLongDelegate(
    val data: ContainerData,
    val index0: Int,
    val index1: Int,
    val index2: Int,
    val index3: Int,
) {
    operator fun getValue(thisRef: Any, property: KProperty<*>): Long {
        return (
            data.get(index0).toUShort().toLong()
            + (data.get(index1).toUShort().toLong() shl 16)
            + (data.get(index2).toUShort().toLong() shl 32)
            + (data.get(index3).toUShort().toLong() shl 48)
        )
    }

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: Long) {
        data.set(index0, value.toInt())
        data.set(index1, (value shr 16).toInt())
        data.set(index2, (value shr 32).toInt())
        data.set(index3, (value shr 48).toInt())
    }
}
