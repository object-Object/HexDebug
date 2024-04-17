package ca.objectobject.hexdebug.debugger

import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.iota.Iota
import net.minecraft.server.level.ServerLevel

data class DebugCastArgs(
    val vm: CastingVM,
    val iotas: List<Iota>,
    val world: ServerLevel,
    val onExecute: ((Iota) -> Unit)? = null
)