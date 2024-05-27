package gay.`object`.hexdebug.casting.eval

import at.petrak.hexcasting.api.spell.Action
import gay.`object`.hexdebug.adapter.DebugAdapterManager
import gay.`object`.hexdebug.debugger.DebugStepType
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import org.eclipse.lsp4j.debug.OutputEventArgumentsCategory

@Suppress("PropertyName")
interface IMixinCastingContext {
    var `isDebugging$hexdebug`: Boolean
    var `lastEvaluatedAction$hexdebug`: Action?
    var `lastDebugStepType$hexdebug`: DebugStepType?
}

fun IMixinCastingContext.reset() {
    `lastEvaluatedAction$hexdebug` = null
    `lastDebugStepType$hexdebug` = null
}

fun printDebugMessage(
    caster: ServerPlayer,
    message: Component,
    category: String = OutputEventArgumentsCategory.STDOUT,
    withSource: Boolean = true,
) {
    DebugAdapterManager[caster]?.print(message.string + "\n", category, withSource)
}

fun printDebugMishap(
    env: CastingContext,
    caster: ServerPlayer,
    mishap: OperatorSideEffect.DoMishap,
) {
    mishap.mishap.errorMessageWithName(env, mishap.errorCtx)?.also {
        printDebugMessage(caster, it, OutputEventArgumentsCategory.STDERR)
    }
}

val CastingVM.debugCastEnv get() = ctx as IMixinCastingContext
