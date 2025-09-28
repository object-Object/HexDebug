@file:JvmName("HexDebugClientAbstractions")
@file:Suppress("UNUSED_PARAMETER")

package gay.`object`.hexdebug

import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.PreparableReloadListener

@ExpectPlatform
fun registerClientResourceReloadListener(id: ResourceLocation, listener: PreparableReloadListener) {
    throw AssertionError()
}

@ExpectPlatform
fun sendHexicalKeyEvent(client: Minecraft, keyCode: Int, scanCode: Int, isPressed: Boolean) {
    throw AssertionError()
}
