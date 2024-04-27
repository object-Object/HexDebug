package gay.`object`.hexdebug.utils

import net.minecraft.world.InteractionHand
import java.util.concurrent.CompletableFuture
import kotlin.enums.enumEntries

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

@OptIn(ExperimentalStdlibApi::class)
inline val <reified T : Enum<T>> T.itemPredicate get() = (ordinal.toFloat() / enumEntries<T>().lastIndex)
