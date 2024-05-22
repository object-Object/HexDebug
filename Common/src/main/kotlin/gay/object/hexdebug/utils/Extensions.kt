package gay.`object`.hexdebug.utils

import at.petrak.hexcasting.api.casting.eval.vm.ContinuationFrame
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation.NotDone
import net.minecraft.world.InteractionHand
import java.util.concurrent.CompletableFuture
import kotlin.enums.enumEntries
import kotlin.math.ceil
import kotlin.math.log
import kotlin.math.pow

// futures

fun <T> T.toFuture(): CompletableFuture<T> = CompletableFuture.completedFuture(this)

fun <T> futureOf(value: T): CompletableFuture<T> = CompletableFuture.completedFuture(value)

fun <T> futureOf(): CompletableFuture<T> = CompletableFuture.completedFuture(null)

// selecting sequence range via start/count (for the debug adapter)

inline fun <reified T> Sequence<T>.paginate(start: Int?, count: Int?): Array<T> {
    var result = this
    if (start != null && start > 0) {
        result = result.drop(start)
    }
    if (count != null && count > 0) {
        result = result.take(count)
    }
    return result.toList().toTypedArray()
}

// items helpers

val InteractionHand.otherHand get() = when (this) {
    InteractionHand.MAIN_HAND -> InteractionHand.OFF_HAND
    InteractionHand.OFF_HAND -> InteractionHand.MAIN_HAND
}

// ceil the denominator to a power of 2 so we don't have issues with eg. 1/3
@OptIn(ExperimentalStdlibApi::class)
inline val <reified T : Enum<T>> T.itemPredicate get() =
    ordinal.toFloat() / (ceil(enumEntries<T>().lastIndex.toFloat() / 2f) * 2f)

inline fun <reified T> List<T>.getWrapping(idx: Int) = this[idx.mod(size)]

/** Rounds `this` up to the next power of `base`. */
fun Number.ceilToPow(base: Number): Int = toDouble().ceilToPow(base.toDouble())

// https://stackoverflow.com/q/19870067
private fun Double.ceilToPow(base: Double): Int = base.pow(ceil(log(this, base))).toInt()

// SpellContinuation

val SpellContinuation.frame get() = (this as? NotDone)?.frame

val SpellContinuation.next get() = (this as? NotDone)?.next

val ContinuationFrame.name get() = this::class.simpleName ?: "Unknown"
