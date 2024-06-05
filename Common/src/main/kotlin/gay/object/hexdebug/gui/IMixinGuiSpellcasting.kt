package gay.`object`.hexdebug.gui

import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.client.gui.GuiSpellcasting
import java.util.function.BiConsumer

@Suppress("PropertyName", "FunctionName")
interface IMixinGuiSpellcasting {
    var `onDrawSplicingTablePattern$hexdebug`: BiConsumer<HexPattern, Int>?

    fun `clearPatterns$hexdebug`()
}

@Suppress("CAST_NEVER_SUCCEEDS")
val GuiSpellcasting.mixin get() = this as IMixinGuiSpellcasting
