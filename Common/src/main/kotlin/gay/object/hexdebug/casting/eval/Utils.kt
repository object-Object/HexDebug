
package gay.`object`.hexdebug.casting.eval

import at.petrak.hexcasting.api.spell.casting.CastingHarness
import gay.`object`.hexdebug.adapter.DebugAdapterManager
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import org.eclipse.lsp4j.debug.OutputEventArgumentsCategory

@Suppress("CAST_NEVER_SUCCEEDS")
val CastingHarness.debugCastEnv get() = ctx as IMixinCastingContext

fun printDebugMessage(
    caster: ServerPlayer,
    message: Component,
    category: String = OutputEventArgumentsCategory.STDOUT,
    withSource: Boolean = true,
) {
    DebugAdapterManager[caster]?.print(message.string + "\n", category, withSource)
}

