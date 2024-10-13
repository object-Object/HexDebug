package gay.`object`.hexdebug.casting.actions

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import gay.`object`.hexdebug.adapter.DebugAdapterManager

object OpNextEvalIndex : ConstMediaAction {
    override val argc = 0

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val (_, index) = DebugAdapterManager[env]
            ?.debugger
            ?.getNextIotaToEvaluate()
            ?: return null.asActionResult
        return index.asActionResult
    }
}
