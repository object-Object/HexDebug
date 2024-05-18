package gay.`object`.hexdebug.casting.eval

import gay.`object`.hexdebug.debugger.DebugStepType
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import org.eclipse.lsp4j.debug.OutputEventArgumentsCategory

class DebugItemCastEnv(
    caster: ServerPlayer,
    castingHand: InteractionHand,
) : PackagedItemCastEnv(caster, castingHand), IDebugCastEnv {
    override var lastEvaluatedAction: Action? = null
    override var lastDebugStepType: DebugStepType? = null

    override fun printMessage(message: Component) {
        super.printMessage(message)
        printDebugMessage(caster, message)
    }

    override fun sendMishapMsgToPlayer(mishap: OperatorSideEffect.DoMishap) {
        super.sendMishapMsgToPlayer(mishap)
        mishap.mishap.errorMessageWithName(this, mishap.errorCtx)?.also {
            printDebugMessage(caster, it, OutputEventArgumentsCategory.STDERR)
        }
    }
}
