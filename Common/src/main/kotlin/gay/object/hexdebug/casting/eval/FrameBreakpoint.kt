@file:Suppress("CAST_NEVER_SUCCEEDS")

package gay.`object`.hexdebug.casting.eval

import at.petrak.hexcasting.api.spell.SpellList
import at.petrak.hexcasting.api.spell.casting.eval.FrameEvaluate

// ContinuationFrame is sealed in this version, so we need to smuggle data on a FrameEvaluate instead
@Suppress("PropertyName")
interface IMixinFrameEvaluate {
    var `isFrameBreakpoint$hexdebug`: Boolean
    var `stopBefore$hexdebug`: Boolean
    var `isFatal$hexdebug`: Boolean
}

fun newFrameBreakpointFatal() = newFrameBreakpoint(stopBefore = true, isFatal = true)

fun newFrameBreakpoint(stopBefore: Boolean, isFatal: Boolean = false): FrameEvaluate {
    return FrameEvaluate(SpellList.LList(listOf()), false).also {
        it as IMixinFrameEvaluate
        it.`isFrameBreakpoint$hexdebug` = true
        it.`stopBefore$hexdebug` = stopBefore
        it.`isFatal$hexdebug` = isFatal
    }
}

val FrameEvaluate.isFrameBreakpoint get() = (this as IMixinFrameEvaluate).`isFrameBreakpoint$hexdebug`
val FrameEvaluate.stopBefore get() = (this as IMixinFrameEvaluate).`stopBefore$hexdebug`
val FrameEvaluate.isFatal get() = (this as IMixinFrameEvaluate).`isFatal$hexdebug`
