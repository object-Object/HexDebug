package gay.`object`.hexdebug.debugger.circles

import at.petrak.hexcasting.api.casting.circles.BlockEntityAbstractImpetus
import gay.`object`.hexdebug.core.api.debugging.DebuggableBlock

@Suppress("FunctionName")
interface IMixinBlockEntityAbstractImpetus : DebuggableBlock {
    fun `hexdebug$clearExecutionState`()
}

val BlockEntityAbstractImpetus.mixin get() = this as IMixinBlockEntityAbstractImpetus
