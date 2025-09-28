@file:JvmName("HexDebugClientAbstractionsImpl")

package gay.`object`.hexdebug.forge

import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import thedarkcolour.kotlinforforge.forge.MOD_BUS

fun registerClientResourceReloadListener(id: ResourceLocation, listener: PreparableReloadListener) {
    MOD_BUS.addListener { event: RegisterClientReloadListenersEvent ->
        event.registerReloadListener(listener)
    }
}
