package gay.`object`.hexdebug.adapter

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import net.minecraft.server.level.ServerLevel

data class CastArgs(
    val iotas: List<Iota>,
    val env: CastingEnvironment,
    val world: ServerLevel,
    val onExecute: ((Iota) -> Unit)? = null
)