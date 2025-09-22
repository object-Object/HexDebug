package gay.`object`.hexdebug.splicing

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.SpecialPatterns
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexPattern
import gay.`object`.hexdebug.utils.displayWithPatternName
import gay.`object`.hexdebug.utils.toHexpatternSource
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import kotlin.math.max

data class SplicingTableClientView(
    val list: List<IotaClientView>?,
    val clipboard: CompoundTag?,
    val isListWritable: Boolean,
    val isClipboardWritable: Boolean,
    val isEnlightened: Boolean,
    val hasHex: Boolean,
    val undoSize: Int,
    val undoIndex: Int,
) {
    val isListReadable = list != null
    val isClipboardReadable = null != clipboard // thanks kotlin

    val lastIndex = list?.lastIndex ?: -1
    val listSize = list?.size ?: 0

    fun isInRange(index: Int) = list?.let { index in it.indices } ?: false

    companion object {
        fun empty() = SplicingTableClientView(
            list = null,
            clipboard = null,
            isListWritable = false,
            isClipboardWritable = false,
            isEnlightened = false,
            hasHex = false,
            undoSize = 0,
            undoIndex = -1,
        )
    }
}

data class IotaClientView(
    val tag: CompoundTag,
    val name: Component,
    val hexpatternSource: String,
    val pattern: HexPattern?,
) {
    constructor(iota: Iota, env: CastingEnvironment) : this(
        tag = IotaType.serialize(iota),
        name = iota.displayWithPatternName(env),
        hexpatternSource = iota.toHexpatternSource(env),
        pattern = (iota as? PatternIota)?.pattern,
    )
}

fun List<IotaClientView>.toHexpatternSource(): String {
    var depth = 0
    return joinToString("\n") {
        if (it.pattern?.angles == SpecialPatterns.RETROSPECTION.angles) depth--
        val indent = " ".repeat(max(0, 4 * depth))
        if (it.pattern?.angles == SpecialPatterns.INTROSPECTION.angles) depth++
        indent + it.hexpatternSource
    }
}
