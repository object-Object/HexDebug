package gay.`object`.hexdebug.gui.splicing

import at.petrak.hexcasting.api.spell.math.HexPattern
import at.petrak.hexcasting.client.gui.GuiSpellcasting
import net.minecraft.world.InteractionHand
import java.util.function.BiConsumer

@Suppress("PropertyName", "FunctionName")
interface IMixinGuiSpellcasting {
    val handOpenedWith: InteractionHand

    var `onDrawSplicingTablePattern$hexdebug`: BiConsumer<HexPattern, Int>?

    fun `clearPatterns$hexdebug`()
}

@Suppress("CAST_NEVER_SUCCEEDS")
val GuiSpellcasting.mixin get() = this as IMixinGuiSpellcasting
