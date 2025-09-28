@file:JvmName("HexDebugAbstractions")
@file:Suppress("UNUSED_PARAMETER")

package gay.`object`.hexdebug

import dev.architectury.injectables.annotations.ExpectPlatform
import gay.`object`.hexdebug.registry.HexDebugRegistrar

fun initRegistries(vararg registries: HexDebugRegistrar<*>) {
    for (registry in registries) {
        initRegistry(registry)
    }
}

@ExpectPlatform
fun <T : Any> initRegistry(registrar: HexDebugRegistrar<T>) {
    throw AssertionError()
}
