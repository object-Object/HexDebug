package gay.`object`.hexdebug.casting.actions

import at.petrak.hexcasting.api.spell.Action
import at.petrak.hexcasting.api.spell.OperationResult
import at.petrak.hexcasting.api.spell.casting.CastingContext
import at.petrak.hexcasting.api.spell.casting.eval.SpellContinuation
import at.petrak.hexcasting.api.spell.iota.Iota
import gay.`object`.hexdebug.casting.eval.newFrameBreakpoint

data class OpBreakpoint(val stopBefore: Boolean) : Action {
    override fun operate(
        continuation: SpellContinuation,
        stack: MutableList<Iota>,
        ravenmind: Iota?,
        ctx: CastingContext
    ): OperationResult {
        val newCont = continuation.pushFrame(newFrameBreakpoint(stopBefore))
        return OperationResult(newCont, stack, ravenmind, listOf())
    }
}
