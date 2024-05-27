package gay.`object`.hexdebug.utils

import at.petrak.hexcasting.xplat.IXplatAbstractions
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import java.util.concurrent.CompletableFuture
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

fun LivingEntity.getItemInHand(hand: InteractionHand, item: Item) =
    getItemInHand(hand).takeIf { it.`is`(item) }

fun LivingEntity.findMediaHolderInHand(hand: InteractionHand, item: Item) =
    getItemInHand(hand, item)?.let(IXplatAbstractions.INSTANCE::findMediaHolder)

// ceil the denominator to a power of 2 so we don't have issues with eg. 1/3
inline fun <reified T : Enum<T>> T.itemPredicate(entries: Array<T>) =
    ordinal.toFloat() / (ceil(entries.lastIndex.toFloat() / 2f) * 2f)

inline fun <reified T> Array<T>.getWrapping(idx: Int) = this[idx.mod(size)]

/** Rounds `this` up to the next power of `base`. */
fun Number.ceilToPow(base: Number): Int = toDouble().ceilToPow(base.toDouble())

// https://stackoverflow.com/q/19870067
private fun Double.ceilToPow(base: Double): Int = base.pow(ceil(log(this, base))).toInt()
