package gay.`object`.hexdebug.debugger

import at.petrak.hexcasting.api.spell.casting.CastingContext
import at.petrak.hexcasting.api.spell.iota.Iota
import net.minecraft.server.level.ServerLevel

data class CastArgs(
    val iotas: List<Iota>,
    val env: CastingContext,
    val world: ServerLevel,
    val onExecute: ((Iota) -> Unit)? = null
)
