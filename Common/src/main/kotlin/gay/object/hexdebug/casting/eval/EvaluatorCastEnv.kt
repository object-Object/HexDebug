package gay.`object`.hexdebug.casting.eval

import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.eval.env.StaffCastEnv
import at.petrak.hexcasting.api.casting.eval.sideeffects.OperatorSideEffect
import gay.`object`.hexdebug.core.api.debugging.DebugStepType
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand

class EvaluatorCastEnv(
    caster: ServerPlayer,
    castingHand: InteractionHand,
) : StaffCastEnv(caster, castingHand), IDebugCastEnv {
    override var threadId: Int? = null

    override var lastEvaluatedAction: Action? = null
    override var lastDebugStepType: DebugStepType? = null

    override fun printMessage(message: Component) {
        super.printMessage(message)
        printDebugMessage(caster, message)
    }

    override fun sendMishapMsgToPlayer(mishap: OperatorSideEffect.DoMishap) {
        super.sendMishapMsgToPlayer(mishap)
        printDebugMishap(this, caster, mishap)
    }
}
