package gay.`object`.hexdebug.utils

import at.petrak.hexcasting.api.PatternRegistry
import at.petrak.hexcasting.api.spell.casting.SpecialPatterns
import at.petrak.hexcasting.api.spell.iota.GarbageIota
import at.petrak.hexcasting.api.spell.iota.Iota
import at.petrak.hexcasting.api.spell.iota.ListIota
import at.petrak.hexcasting.api.spell.iota.PatternIota
import at.petrak.hexcasting.api.spell.math.HexPattern
import at.petrak.hexcasting.api.spell.mishaps.MishapInvalidPattern
import at.petrak.hexcasting.api.utils.*
import at.petrak.hexcasting.xplat.IXplatAbstractions
import net.minecraft.network.chat.*
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Container
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import java.awt.Color
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.math.ceil

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
inline fun <reified T : Enum<T>> T.asItemPredicate(entries: Array<T>) =
    ordinal.toFloat() / (ceil(entries.lastIndex.toFloat() / 2f) * 2f)

val Boolean.asItemPredicate get() = if (this) 1f else 0f

inline fun <reified T> Array<T>.getWrapping(idx: Int) = this[idx.mod(size)]

// iota/pattern stringifying

fun Iota.displayWithPatternName(world: ServerLevel): Component = when (this) {
    is PatternIota -> pattern.getI18nOrNull(world) ?: display()
    is ListIota -> {
        val contents = list.toList().joinToComponent(", ") { it.displayWithPatternName(world) }
        "hexcasting.tooltip.list_contents".asTranslatedComponent(contents).darkPurple
    }
    else -> display()
}

fun Iota.toHexpatternSource(world: ServerLevel, wrapEmbedded: Boolean = true): String {
    val iotaText = when (this) {
        is PatternIota -> {
            // don't wrap known patterns in angled brackets
            when (pattern) {
                SpecialPatterns.INTROSPECTION -> "{"
                SpecialPatterns.RETROSPECTION -> "}"
                else -> pattern.getI18nOrNull(world)?.string
            }?.let { return it }
            // but do wrap unknown ones
            pattern.simpleString()
        }
        is ListIota -> list.joinToString(separator = ", ", prefix = "[", postfix = "]") {
            when (it) {
                // don't use { and } for intro/retro in an embedded list
                is PatternIota -> it.pattern.getI18nOrNull(world)?.string ?: it.pattern.simpleString()
                else -> it.toHexpatternSource(world, wrapEmbedded = false)
            }
        }
        is GarbageIota -> "Garbage"
        else -> display().string
    }
    if (wrapEmbedded) {
        return "<$iotaText>"
    }
    return iotaText
}

fun HexPattern.getI18nOrNull(world: ServerLevel): Component? {
    return try {
        PatternRegistry.matchPattern(this, world).displayName
    } catch (e: MishapInvalidPattern) {
        val path = when (this) {
            SpecialPatterns.INTROSPECTION -> "open_paren"
            SpecialPatterns.RETROSPECTION -> "close_paren"
            SpecialPatterns.CONSIDERATION -> "escape"
            else -> return null
        }
        "hexcasting.spell.hexcasting:$path".asTranslatedComponent.lightPurple
    }
}

/** Format: `START_DIR signature` (eg. `EAST`, `NORTH_WEST aqwed`) */
fun HexPattern.simpleString() = buildString {
    append(startDir)
    if (angles.isNotEmpty()) {
        append(" ")
        append(anglesSignature())
    }
}

val Color.fred get() = red.toFloat() / 255f
val Color.fgreen get() = green.toFloat() / 255f
val Color.fblue get() = blue.toFloat() / 255f
val Color.falpha get() = alpha.toFloat() / 255f
