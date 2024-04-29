package gay.`object`.hexdebug.registry

import at.petrak.hexcasting.api.casting.eval.vm.ContinuationFrame
import at.petrak.hexcasting.common.lib.HexRegistries
import at.petrak.hexcasting.common.lib.hex.HexContinuationTypes
import gay.`object`.hexdebug.casting.eval.FrameBreakpoint

object HexDebugContinuationTypes : HexDebugRegistrar<ContinuationFrame.Type<*>>(
    HexRegistries.CONTINUATION_TYPE,
    { HexContinuationTypes.REGISTRY },
) {
    val BREAKPOINT = register("breakpoint") { FrameBreakpoint.TYPE }
}
