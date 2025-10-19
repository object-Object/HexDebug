package gay.`object`.hexdebug.impl

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.iota.Iota
import gay.`object`.hexdebug.adapter.DebugAdapter
import gay.`object`.hexdebug.adapter.DebugAdapterManager
import gay.`object`.hexdebug.core.api.HexDebugCoreAPI
import gay.`object`.hexdebug.core.api.debugging.DebugEnvironment
import gay.`object`.hexdebug.core.api.debugging.DebugOutputCategory
import gay.`object`.hexdebug.core.api.exceptions.IllegalDebugSessionException
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import org.eclipse.lsp4j.debug.OutputEventArgumentsCategory
import java.util.*

class HexDebugCoreAPIImpl : HexDebugCoreAPI {
    override fun getDebugEnv(env: CastingEnvironment): DebugEnvironment? {
        return (env as IDebugEnvAccessor).`debugEnv$hexdebug`
    }

    override fun getDebugEnv(caster: ServerPlayer, sessionId: UUID): DebugEnvironment? {
        return DebugAdapterManager[caster]?.debugger(sessionId)?.debugEnv
    }

    override fun createDebugThread(debugEnv: DebugEnvironment, threadId: Int?) {
        getAdapterOrThrow(debugEnv).createDebugThread(debugEnv, threadId)
    }

    override fun startExecuting(
        debugEnv: DebugEnvironment,
        env: CastingEnvironment,
        iotas: MutableList<Iota>,
        image: CastingImage?,
    ) {
        getAdapterOrThrow(debugEnv).startExecuting(debugEnv, env, iotas, image)
    }

    override fun removeDebugThread(debugEnv: DebugEnvironment) {
        DebugAdapterManager[debugEnv.caster]?.removeThread(debugEnv.sessionId, terminate = false)
    }

    override fun terminateDebugThread(debugEnv: DebugEnvironment) {
        DebugAdapterManager[debugEnv.caster]?.removeThread(debugEnv.sessionId, terminate = true)
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

    private fun getAdapterOrThrow(debugEnv: DebugEnvironment): DebugAdapter {
        return DebugAdapterManager[debugEnv.caster]
            ?: throw IllegalDebugSessionException("Debug adapter not found for ${debugEnv.caster}")
    }
}

@Suppress("PropertyName")
interface IDebugEnvAccessor {
    var `debugEnv$hexdebug`: DebugEnvironment?
}
