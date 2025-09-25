@file:JvmName("HexDebugAbstractions")
@file:Suppress("UNUSED_PARAMETER")

package gay.`object`.hexdebug

import dev.architectury.injectables.annotations.ExpectPlatform
import gay.`object`.hexdebug.registry.HexDebugRegistrar
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.PreparableReloadListener

fun initRegistries(vararg registries: HexDebugRegistrar<*>) {
    for (registry in registries) {
        initRegistry(registry)
    }
}

@ExpectPlatform
fun <T : Any> initRegistry(registrar: HexDebugRegistrar<T>) {
    throw AssertionError()
}

@ExpectPlatform
fun registerClientResourceReloadListener(id: ResourceLocation, listener: PreparableReloadListener) {
    throw AssertionError()
}
