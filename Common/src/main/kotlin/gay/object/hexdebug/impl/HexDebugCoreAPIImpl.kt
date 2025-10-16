package gay.`object`.hexdebug.impl

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import gay.`object`.hexdebug.adapter.DebugAdapterManager
import gay.`object`.hexdebug.core.api.HexDebugCoreAPI
import gay.`object`.hexdebug.core.api.debugging.DebugEnvironment
import gay.`object`.hexdebug.core.api.debugging.DebugOutputCategory
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import org.eclipse.lsp4j.debug.OutputEventArgumentsCategory
import java.util.*

class HexDebugCoreAPIImpl : HexDebugCoreAPI {
    override fun getDebugEnv(env: CastingEnvironment): DebugEnvironment? {
        return (env as IDebugEnvAccessor).`debugEnv$hexdebug`
    }

    override fun printDebugMessage(
        caster: ServerPlayer,
        sessionId: UUID,
        message: Component,
        category: DebugOutputCategory,
        withSource: Boolean,
    ) {
        val categoryStr = when (category) {
            DebugOutputCategory.CONSOLE -> OutputEventArgumentsCategory.CONSOLE
            DebugOutputCategory.IMPORTANT -> OutputEventArgumentsCategory.IMPORTANT
            DebugOutputCategory.STDOUT -> OutputEventArgumentsCategory.STDOUT
            DebugOutputCategory.STDERR -> OutputEventArgumentsCategory.STDERR
            DebugOutputCategory.TELEMETRY -> OutputEventArgumentsCategory.TELEMETRY
        }
        DebugAdapterManager[caster]?.print(sessionId, message.string + "\n", categoryStr, withSource)
    }
}

@Suppress("PropertyName")
interface IDebugEnvAccessor {
    var `debugEnv$hexdebug`: DebugEnvironment?
}
