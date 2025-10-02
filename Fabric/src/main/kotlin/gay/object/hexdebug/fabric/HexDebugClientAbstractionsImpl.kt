@file:JvmName("HexDebugClientAbstractionsImpl")

package gay.`object`.hexdebug.fabric

import gay.`object`.hexdebug.fabric.interop.FabricHexDebugHexicalInterop
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft

fun sendHexicalKeyEvent(client: Minecraft, keyCode: Int, scanCode: Int, isPressed: Boolean) {
    if (FabricLoader.getInstance().isModLoaded("hexical")) {
        FabricHexDebugHexicalInterop.sendKeyEvent(client, keyCode, scanCode, isPressed)
    }
}
