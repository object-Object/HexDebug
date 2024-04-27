package gay.`object`.hexdebug.casting.actions

import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.OperationResult
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import gay.`object`.hexdebug.casting.eval.FrameBreakpoint

data class OpBreakpoint(val type: Type) : Action {
    override fun operate(
        env: CastingEnvironment,
        image: CastingImage,
        continuation: SpellContinuation
    ): OperationResult {
        val newCont = when (type) {
            Type.BEFORE -> continuation // handled in HexDebugger
            Type.AFTER -> continuation.pushFrame(FrameBreakpoint)
        }
        return OperationResult(image.withUsedOp(), listOf(), newCont, HexEvalSounds.NORMAL_EXECUTE)
    }

    enum class Type {
        BEFORE,
        AFTER,
    }
}
