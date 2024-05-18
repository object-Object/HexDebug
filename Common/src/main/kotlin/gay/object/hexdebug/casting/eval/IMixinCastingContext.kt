package gay.`object`.hexdebug.casting.eval

import at.petrak.hexcasting.api.spell.Action
import gay.`object`.hexdebug.adapter.DebugAdapterManager
import gay.`object`.hexdebug.debugger.DebugStepType
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import org.eclipse.lsp4j.debug.OutputEventArgumentsCategory

@Suppress("PropertyName", "FunctionName")
interface IMixinCastingContext {
    var `isDebugging$hexdebug`: Boolean

    var `lastEvaluatedAction$hexdebug`: Action?
    var `lastDebugStepType$hexdebug`: DebugStepType?

    fun `reset$hexdebug`() {
        `lastEvaluatedAction$hexdebug` = null
        `lastDebugStepType$hexdebug` = null
    }

    fun `printDebugMessage$hexdebug`(
        caster: ServerPlayer,
        message: Component,
        category: String = OutputEventArgumentsCategory.STDOUT,
        withSource: Boolean = true,
    ) {
        DebugAdapterManager[caster]?.print(message.string + "\n", category, withSource)
    }
}
