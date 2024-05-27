@file:Suppress("CAST_NEVER_SUCCEEDS")

package gay.`object`.hexdebug.casting.eval

import at.petrak.hexcasting.api.spell.Action
import at.petrak.hexcasting.api.spell.casting.CastingContext
import at.petrak.hexcasting.api.spell.casting.CastingContext.CastSource
import at.petrak.hexcasting.api.spell.casting.CastingHarness
import at.petrak.hexcasting.api.spell.casting.sideeffects.OperatorSideEffect
import gay.`object`.hexdebug.adapter.DebugAdapterManager
import gay.`object`.hexdebug.debugger.DebugStepType
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import org.eclipse.lsp4j.debug.OutputEventArgumentsCategory

@Suppress("PropertyName")
interface IMixinCastingContext {
    var `isDebugging$hexdebug`: Boolean
    var `debugCastEnvType$hexdebug`: DebugCastEnvType?
    var `lastEvaluatedAction$hexdebug`: Action?
    var `lastDebugStepType$hexdebug`: DebugStepType?
}

fun IMixinCastingContext.reset() {
    `lastEvaluatedAction$hexdebug` = null
    `lastDebugStepType$hexdebug` = null
}

fun newDebuggerCastEnv(caster: ServerPlayer, castingHand: InteractionHand): CastingContext {
    return CastingContext(caster, castingHand, CastSource.PACKAGED_HEX).apply {
        this as IMixinCastingContext
        `isDebugging$hexdebug` = true
        `debugCastEnvType$hexdebug` = DebugCastEnvType.DEBUGGER
    }
}

fun newEvaluatorCastEnv(caster: ServerPlayer, castingHand: InteractionHand): CastingContext {
    return CastingContext(caster, castingHand, CastSource.STAFF).apply {
        this as IMixinCastingContext
        `isDebugging$hexdebug` = true
        `debugCastEnvType$hexdebug` = DebugCastEnvType.EVALUATOR
    }
}

