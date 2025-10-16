package gay.`object`.hexdebug.casting.actions

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import gay.`object`.hexdebug.core.api.HexDebugCoreAPI

object OpIsDebugging : ConstMediaAction {
    override val argc = 0

    override fun execute(args: List<Iota>, env: CastingEnvironment) =
        (HexDebugCoreAPI.INSTANCE.getDebugEnv(env) != null).asActionResult
}
