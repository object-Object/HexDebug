package gay.`object`.hexdebug.casting.actions

import at.petrak.hexcasting.api.spell.ConstMediaAction
import at.petrak.hexcasting.api.spell.asActionResult
import at.petrak.hexcasting.api.spell.casting.CastingContext
import at.petrak.hexcasting.api.spell.iota.Iota
import gay.`object`.hexdebug.adapter.DebugAdapterManager

object OpNextEvalIndex : ConstMediaAction {
    override val argc = 0

    override fun execute(args: List<Iota>, ctx: CastingContext): List<Iota> {
        val (_, index) = DebugAdapterManager[ctx]
            ?.debugger
            ?.getNextIotaToEvaluate()
            ?: return null.asActionResult
        return index.asActionResult
    }
}
