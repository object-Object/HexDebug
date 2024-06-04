package gay.`object`.hexdebug.gui

import at.petrak.hexcasting.api.casting.math.HexPattern
import java.util.function.BiConsumer

@Suppress("PropertyName")
interface IMixinGuiSpellcasting {
    var `onDrawSplicingTablePattern$hexdebug`: BiConsumer<HexPattern, Int>?
}
