package gay.`object`.hexdebug.gui

import net.minecraft.client.gui.screens.Screen

@Suppress("FunctionName")
interface IMixinScreen {
    fun `hexdebug$setPreviousScreen`(screen: Screen?)
}

@Suppress("CAST_NEVER_SUCCEEDS")
val Screen.mixin get() = this as IMixinScreen
