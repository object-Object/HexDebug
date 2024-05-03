package gay.`object`.hexdebug.casting.actions

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import gay.`object`.hexdebug.casting.eval.IDebugCastEnv

object OpIsDebugging : ConstMediaAction {
    override val argc = 0

    override fun execute(args: List<Iota>, env: CastingEnvironment) = when (env) {
        is IDebugCastEnv -> env.isDebugging
        else -> false
    }.asActionResult
}
