package gay.`object`.hexdebug.casting.eval

import gay.`object`.hexdebug.adapter.DebugAdapterManager
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import org.eclipse.lsp4j.debug.OutputEventArgumentsCategory

interface IDebugCastEnv {
    val isDebugging get() = true

    fun printDebugMessage(
        caster: ServerPlayer,
        message: Component,
        category: String = OutputEventArgumentsCategory.STDOUT,
    ) {
        DebugAdapterManager[caster]?.print(message.string + "\n", category)
    }
}
