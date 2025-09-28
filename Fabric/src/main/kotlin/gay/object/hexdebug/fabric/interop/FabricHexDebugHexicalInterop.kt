package gay.`object`.hexdebug.fabric.interop

import at.petrak.hexcasting.api.HexAPI
import gay.`object`.hexdebug.gui.mixin
import miyucomics.hexical.features.telepathy.ServerPeripheralReceiver
import miyucomics.hexical.inits.HexicalKeybinds
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import vazkii.patchouli.api.PatchouliAPI

object FabricHexDebugHexicalInterop {
    private val previousState = mutableMapOf<String, Boolean>()

    fun sendKeyEvent(client: Minecraft, keyCode: Int, scanCode: Int, isPressed: Boolean) {
        if (client.player == null) return

        // open the notebook gui

        if (HexicalKeybinds.OPEN_HEXBOOK.matches(keyCode, scanCode)) {
            val previousScreen = client.screen
            PatchouliAPI.get().openBookGUI(HexAPI.modLoc("thehexbook"))
            client.screen?.mixin?.`hexdebug$setPreviousScreen`(previousScreen)
        }

        // send telepathy packets as a workaround for telepathy not working in GUIs
        // https://github.com/miyucomics/hexical/blob/fbcbb09547bcb63da892a7ed2568960e7f126234/src/client/java/miyucomics/hexical/features/telepathy/ClientPeripheralPusher.kt#L15

        val key = getTelepathyKey(client, keyCode, scanCode) ?: return

        if (previousState[key.name] != isPressed) {
            previousState[key.name] = isPressed
            val channel = when (isPressed) {
                true -> ServerPeripheralReceiver.PRESSED_KEY_CHANNEL
                false -> ServerPeripheralReceiver.RELEASED_KEY_CHANNEL
            }
            ClientPlayNetworking.send(channel, PacketByteBufs.create().also { it.writeUtf(key.name) })
        }
    }

    private fun getTelepathyKey(client: Minecraft, keyCode: Int, scanCode: Int): KeyMapping? {
        for (key in arrayOf(
            client.options.keyUp,
            client.options.keyLeft,
            client.options.keyRight,
            client.options.keyDown,
            client.options.keyJump,
            client.options.keyShift,
            client.options.keyUse,
            client.options.keyAttack,
            HexicalKeybinds.TELEPATHY_KEYBIND,
            // omitted evocation
        )) {
            if (key.matches(keyCode, scanCode)) {
                return key
            }
        }
        return null
    }
}
