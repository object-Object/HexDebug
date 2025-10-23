package gay.`object`.hexdebug.impl

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.iota.Iota
import gay.`object`.hexdebug.adapter.DebugAdapter
import gay.`object`.hexdebug.adapter.DebugAdapterManager
import gay.`object`.hexdebug.core.api.HexDebugCoreAPI
import gay.`object`.hexdebug.core.api.debugging.OutputCategory
import gay.`object`.hexdebug.core.api.debugging.env.DebugEnvironment
import gay.`object`.hexdebug.core.api.exceptions.IllegalDebugSessionException
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import org.eclipse.lsp4j.debug.OutputEventArgumentsCategory
import java.util.*

class HexDebugCoreAPIImpl : HexDebugCoreAPI {
    override fun getDebugEnv(env: CastingEnvironment): DebugEnvironment? {
        return (env as IDebugEnvAccessor).`debugEnv$hexdebug`
    }

    override fun getDebugEnv(casterId: UUID, sessionId: UUID): DebugEnvironment? {
        return DebugAdapterManager[casterId]?.debugger(sessionId)?.debugEnv
    }

    override fun getDebugEnv(casterId: UUID, threadId: Int): DebugEnvironment? {
        return DebugAdapterManager[casterId]?.debugger(threadId)?.debugEnv
    }

    override fun createDebugThread(debugEnv: DebugEnvironment, threadId: Int?) {
        getAdapterOrThrow(debugEnv).createDebugThread(debugEnv, threadId)
    }

    override fun startDebuggingIotas(
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
        category: OutputCategory,
        withSource: Boolean,
    ) {
        val categoryStr = when (category) {
            OutputCategory.CONSOLE -> OutputEventArgumentsCategory.CONSOLE
            OutputCategory.IMPORTANT -> OutputEventArgumentsCategory.IMPORTANT
            OutputCategory.STDOUT -> OutputEventArgumentsCategory.STDOUT
            OutputCategory.STDERR -> OutputEventArgumentsCategory.STDERR
            OutputCategory.TELEMETRY -> OutputEventArgumentsCategory.TELEMETRY
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
