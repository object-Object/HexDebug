package gay.`object`.hexdebug.casting.eval

import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.sideeffects.OperatorSideEffect
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import gay.`object`.hexdebug.adapter.DebugAdapterManager
import gay.`object`.hexdebug.debugger.DebugStepType
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import org.eclipse.lsp4j.debug.OutputEventArgumentsCategory

interface IDebugCastEnv {
    val isDebugging get() = true

    var lastEvaluatedAction: Action?
    var lastDebugStepType: DebugStepType?

    fun reset() {
        lastEvaluatedAction = null
        lastDebugStepType = null
    }

    fun printDebugMessage(
        caster: ServerPlayer,
        message: Component,
        category: String = OutputEventArgumentsCategory.STDOUT,
        withSource: Boolean = true,
    ) {
        DebugAdapterManager[caster]?.print(message.string + "\n", category, withSource)
    }

    fun printDebugMishap(env: CastingEnvironment, caster: ServerPlayer, mishap: OperatorSideEffect.DoMishap) {
        mishap.mishap.errorMessageWithName(env, mishap.errorCtx)?.also {
            printDebugMessage(caster, it, OutputEventArgumentsCategory.STDERR)
        }
    }
}

val CastingVM.debugCastEnv get() = env as IDebugCastEnv
