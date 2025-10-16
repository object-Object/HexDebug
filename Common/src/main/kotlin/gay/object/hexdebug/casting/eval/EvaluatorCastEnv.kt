package gay.`object`.hexdebug.casting.eval

import at.petrak.hexcasting.api.casting.eval.env.StaffCastEnv
import at.petrak.hexcasting.api.casting.eval.sideeffects.OperatorSideEffect
import gay.`object`.hexdebug.core.api.debugging.IDebuggableCastEnv
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand

class EvaluatorCastEnv(
    caster: ServerPlayer,
    castingHand: InteractionHand,
) : StaffCastEnv(caster, castingHand), IDebuggableCastEnv {
    override fun printMessage(message: Component) {
        super.printMessage(message)
        debugEnv?.printDebugMessage(message)
    }

    override fun sendMishapMsgToPlayer(mishap: OperatorSideEffect.DoMishap) {
        super.sendMishapMsgToPlayer(mishap)
        debugEnv?.printDebugMishap(this, mishap)
    }
}
