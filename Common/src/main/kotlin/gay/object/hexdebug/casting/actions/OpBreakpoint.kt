package gay.`object`.hexdebug.casting.actions

import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.OperationResult
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import gay.`object`.hexdebug.casting.eval.FrameBreakpoint

data class OpBreakpoint(val stopBefore: Boolean) : Action {
    override fun operate(
        env: CastingEnvironment,
        image: CastingImage,
        continuation: SpellContinuation
    ): OperationResult {
        val newCont = continuation.pushFrame(FrameBreakpoint(stopBefore))
        return OperationResult(image.withUsedOp(), listOf(), newCont, HexEvalSounds.NORMAL_EXECUTE)
    }
}
