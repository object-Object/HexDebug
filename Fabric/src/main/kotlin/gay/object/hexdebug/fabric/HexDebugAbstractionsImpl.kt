@file:JvmName("HexDebugAbstractionsImpl")

package gay.`object`.hexdebug.fabric

import gay.`object`.hexdebug.registry.HexDebugRegistrar
import net.minecraft.core.Registry

fun <T : Any> initRegistry(registrar: HexDebugRegistrar<T>) {
    val registry = registrar.registry.value
    registrar.init { id, value -> Registry.register(registry, id, value) }
}
