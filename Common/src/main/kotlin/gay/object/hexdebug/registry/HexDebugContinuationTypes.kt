package gay.`object`.hexdebug.registry

import at.petrak.hexcasting.api.casting.eval.vm.ContinuationFrame
import at.petrak.hexcasting.common.lib.hex.HexContinuationTypes
import gay.`object`.hexdebug.casting.eval.FrameBreakpoint

object HexDebugContinuationTypes : HexDebugRegistry<ContinuationFrame.Type<*>>(HexContinuationTypes.REGISTRY) {
    val BREAKPOINT by register("breakpoint") { FrameBreakpoint.TYPE }
}
