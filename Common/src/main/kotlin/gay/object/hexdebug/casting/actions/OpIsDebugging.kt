package gay.`object`.hexdebug.casting.actions

import at.petrak.hexcasting.api.spell.ConstMediaAction
import at.petrak.hexcasting.api.spell.asActionResult
import at.petrak.hexcasting.api.spell.casting.CastingContext
import at.petrak.hexcasting.api.spell.iota.Iota
import gay.`object`.hexdebug.casting.eval.IMixinCastingContext

object OpIsDebugging : ConstMediaAction {
    override val argc = 0

    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun execute(args: List<Iota>, ctx: CastingContext) =
        ((ctx as? IMixinCastingContext)?.`isDebugging$hexdebug` ?: false).asActionResult
}
