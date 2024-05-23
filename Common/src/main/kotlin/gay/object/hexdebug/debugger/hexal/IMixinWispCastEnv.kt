package gay.`object`.hexdebug.debugger.hexal

import gay.`object`.hexdebug.casting.eval.IDebugCastEnv

interface IMixinWispCastEnv : IDebugCastEnv {
    override var isDebugging: Boolean
}
