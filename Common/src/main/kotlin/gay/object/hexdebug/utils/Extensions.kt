package gay.`object`.hexdebug.utils

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.casting.PatternShapeMatch
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.SpecialPatterns
import at.petrak.hexcasting.api.casting.iota.*
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.api.casting.mishaps.MishapNotEnoughArgs
import at.petrak.hexcasting.api.utils.*
import at.petrak.hexcasting.common.casting.PatternRegistryManifest
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.api.splicing.SplicingTableIotaClientView
import net.minecraft.network.chat.*
import net.minecraft.world.Container
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.properties.Property
import java.awt.Color
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.enums.enumEntries
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

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

val Boolean.asItemPredicate get() = if (this) 1f else 0f

inline fun <reified T> List<T>.getWrapping(idx: Int) = this[idx.mod(size)]

// iota/pattern stringifying

fun Iota.displayWithPatternName(env: CastingEnvironment): Component = when (this) {
    is PatternIota -> pattern.getI18nOrNull(env) ?: display()
    is ListIota -> {
        val contents = list.toList().joinToComponent(", ") { it.displayWithPatternName(env) }
        "hexcasting.tooltip.list_contents".asTranslatedComponent(contents).darkPurple
    }
    else -> display()
}

@JvmOverloads
fun Iota.toHexpatternSource(env: CastingEnvironment, wrapEmbedded: Boolean = true): String {
    val iotaText = when (this) {
        is PatternIota -> {
            // don't wrap known patterns in angled brackets
            when (pattern.angles) {
                SpecialPatterns.INTROSPECTION.angles -> "{"
                SpecialPatterns.RETROSPECTION.angles -> "}"
                else -> pattern.getI18nOrNull(env)?.string
            }?.let { return it }
            // but do wrap unknown ones
            pattern.simpleString()
        }
        is ListIota -> list.joinToString(separator = ", ", prefix = "[", postfix = "]") {
            when (it) {
                // don't use { and } for intro/retro in an embedded list
                is PatternIota -> it.pattern.getI18nOrNull(env)?.string ?: it.pattern.simpleString()
                else -> it.toHexpatternSource(env, wrapEmbedded = false)
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

fun SplicingTableIotaClientView.deserializePattern(): HexPattern? {
    return try {
        PatternIota.deserialize(data).pattern
    } catch (_: Exception) {
        null
    }
}

fun List<SplicingTableIotaClientView>.toHexpatternSource(): String {
    var depth = 0
    return joinToString("\n") {
        val pattern = it.deserializePattern()
        if (pattern?.angles == SpecialPatterns.RETROSPECTION.angles) depth--
        val indent = " ".repeat(max(0, 4 * depth))
        if (pattern?.angles == SpecialPatterns.INTROSPECTION.angles) depth++
        indent + it.hexpatternSource
    }
}

fun HexPattern.getI18nOrNull(env: CastingEnvironment): Component? {
    val hexAPI = HexAPI.instance()
    return when (val lookup = PatternRegistryManifest.matchPattern(this, env, false)) {
        is PatternShapeMatch.Normal -> hexAPI.getActionI18n(lookup.key, false)
        is PatternShapeMatch.PerWorld -> hexAPI.getActionI18n(lookup.key, true)
        is PatternShapeMatch.Special -> lookup.handler.name
        is PatternShapeMatch.Nothing -> {
            val path = when (this.angles) {
                SpecialPatterns.INTROSPECTION.angles -> "open_paren"
                SpecialPatterns.RETROSPECTION.angles -> "close_paren"
                SpecialPatterns.CONSIDERATION.angles -> "escape"
                SpecialPatterns.EVANITION.angles -> "undo"
                else -> return null
            }
            hexAPI.getRawHookI18n(HexAPI.modLoc(path))
        }
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

// action helpers

fun List<Iota>.getPositiveIntOrNull(idx: Int, argc: Int = 0): Int? {
    val x = this.getOrElse(idx) { throw MishapNotEnoughArgs(idx + 1, this.size) }
    when (x) {
        is DoubleIota -> {
            val double = x.double
            val rounded = double.roundToInt()
            if (abs(double - rounded) <= DoubleIota.TOLERANCE && rounded >= 0) {
                return rounded
            }
        }
        is NullIota -> return null
    }
    throw MishapInvalidIota.of(x, if (argc == 0) idx else argc - (idx + 1), "int.positive_or_null")
}

// block entities

fun <T : Comparable<T>, V : T> BlockEntity.setPropertyIfChanged(property: Property<T>, value: V) {
    if (blockState.getValue(property) != value) {
        level?.setBlockAndUpdate(blockPos, blockState.setValue(property, value))
    }
}
