package gay.`object`.hexdebug.debugger

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import gay.`object`.hexdebug.core.api.debugging.DebugEnvironment
import net.minecraft.server.level.ServerLevel

data class CastArgs(
    val iotas: List<Iota>,
    val debugEnv: DebugEnvironment,
    val env: CastingEnvironment,
    val world: ServerLevel,
    val onExecute: ((Iota) -> Unit)? = null,
)
