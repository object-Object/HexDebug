@file:JvmName("HexDebugClientAbstractions")
@file:Suppress("UNUSED_PARAMETER")

package gay.`object`.hexdebug

import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.client.Minecraft

@ExpectPlatform
fun sendHexicalKeyEvent(client: Minecraft, keyCode: Int, scanCode: Int, isPressed: Boolean) {
    throw AssertionError()
}
