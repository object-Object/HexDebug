@file:JvmName("HexDebugAbstractionsImpl")

package gay.`object`.hexdebug.forge

import gay.`object`.hexdebug.registry.HexDebugRegistrar
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import net.minecraftforge.registries.RegisterEvent
import thedarkcolour.kotlinforforge.forge.MOD_BUS

fun <T : Any> initRegistry(registrar: HexDebugRegistrar<T>) {
    MOD_BUS.addListener { event: RegisterEvent ->
        event.register(registrar.registryKey) { helper ->
            registrar.init(helper::register)
        }
    }
}

fun registerClientResourceReloadListener(id: ResourceLocation, listener: PreparableReloadListener) {
    MOD_BUS.addListener { event: RegisterClientReloadListenersEvent ->
        event.registerReloadListener(listener)
    }
}
