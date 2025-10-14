@file:JvmName("HexDebugClientAbstractionsImpl")

package gay.`object`.hexdebug.fabric

import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.fabric.interop.FabricHexDebugHexicalInterop
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.Version
import net.minecraft.client.Minecraft
import kotlin.jvm.optionals.getOrNull

private val HEXICAL_MIN_VERSION: Version = Version.parse("2.0.0")
private var hasLoggedWarning = false

fun sendHexicalKeyEvent(client: Minecraft, keyCode: Int, scanCode: Int, isPressed: Boolean) {
    val hexical = FabricLoader.getInstance().getModContainer("hexical").getOrNull() ?: return
    val hexicalVersion = hexical.metadata.version
    if (hexicalVersion >= HEXICAL_MIN_VERSION) {
        FabricHexDebugHexicalInterop.sendKeyEvent(client, keyCode, scanCode, isPressed)
    } else if (!hasLoggedWarning) {
        HexDebug.LOGGER.error("Hexical splicing table interop requires hexical>=${HEXICAL_MIN_VERSION.friendlyString}, but found ${hexicalVersion.friendlyString}. Disable Hexical interop in HexDebug's client config to suppress this message.")
        hasLoggedWarning = true
    }
}
