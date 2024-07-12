package gay.`object`.hexdebug.utils

import at.petrak.hexcasting.api.utils.asTextComponent
import at.petrak.hexcasting.api.utils.italic
import at.petrak.hexcasting.api.utils.plusAssign
import at.petrak.hexcasting.api.utils.styledWith
import at.petrak.hexcasting.xplat.IXplatAbstractions
import net.minecraft.network.chat.*
import net.minecraft.world.Container
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import java.util.*
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

fun LivingEntity.getItemInHand(hand: InteractionHand, item: Item) =
    getItemInHand(hand).takeIf { it.`is`(item) }

fun LivingEntity.findMediaHolderInHand(hand: InteractionHand, item: Item) =
    getItemInHand(hand, item)?.let(IXplatAbstractions.INSTANCE::findMediaHolder)

val Container.isNotEmpty get() = !isEmpty

val ItemStack.isNotEmpty get() = !isEmpty

val ItemStack.styledHoverName: Component get() = Component.empty()
    .append(hoverName)
    .withStyle(rarity.color)
    .also { if (hasCustomHoverName()) it.italic }

// text

fun FormattedText.toComponent(): Component {
    if (this is Component) return this
    val result = Component.empty()
    visit({ style, string ->
        result += string.asTextComponent styledWith style
        Optional.empty<Void>()
    }, Style.EMPTY)
    return result
}

operator fun MutableComponent.plusAssign(string: String) {
    append(string)
}

fun componentOf(value: Any?): Component = when (value) {
    is Component -> value
    is String -> value.asTextComponent
    else -> value.toString().asTextComponent
}

fun Collection<Component>.joinToComponent(separator: String) = joinToComponent(separator.asTextComponent)

fun Collection<Component>.joinToComponent(separator: Component = ComponentUtils.DEFAULT_SEPARATOR): Component {
    return ComponentUtils.formatList(this, separator)
}

fun <T> Collection<T>.joinToComponent(
    separator: String,
    componentExtractor: (T) -> Component = ::componentOf,
) = joinToComponent(separator.asTextComponent, componentExtractor)

fun <T> Collection<T>.joinToComponent(
    separator: Component = ComponentUtils.DEFAULT_SEPARATOR,
    componentExtractor: (T) -> Component = ::componentOf,
): Component {
    return ComponentUtils.formatList(this, separator, componentExtractor)
}

// ceil the denominator to a power of 2 so we don't have issues with eg. 1/3
@OptIn(ExperimentalStdlibApi::class)
inline val <reified T : Enum<T>> T.asItemPredicate get() =
    ordinal.toFloat() / (ceil(enumEntries<T>().lastIndex.toFloat() / 2f) * 2f)

inline fun <reified T> List<T>.getWrapping(idx: Int) = this[idx.mod(size)]

/** Rounds `this` up to the next power of `base`. */
fun Number.ceilToPow(base: Number): Int = toDouble().ceilToPow(base.toDouble())

// https://stackoverflow.com/q/19870067
private fun Double.ceilToPow(base: Double): Int = base.pow(ceil(log(this, base))).toInt()

val Boolean.asItemPredicate get() = if (this) 1f else 0f
