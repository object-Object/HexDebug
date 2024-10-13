package gay.`object`.hexdebug.splicing

import at.petrak.hexcasting.api.spell.casting.SpecialPatterns
import at.petrak.hexcasting.api.spell.iota.Iota
import at.petrak.hexcasting.api.spell.iota.PatternIota
import at.petrak.hexcasting.api.spell.math.HexPattern
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes
import gay.`object`.hexdebug.utils.displayWithPatternName
import gay.`object`.hexdebug.utils.toHexpatternSource
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import kotlin.math.max

data class SplicingTableClientView(
    val list: List<IotaClientView>?,
    val clipboard: CompoundTag?,
    val isListWritable: Boolean,
    val isClipboardWritable: Boolean,
    val undoSize: Int,
    val undoIndex: Int,
) {
    val isListReadable = list != null
    val isClipboardReadable = null != clipboard // thanks kotlin

    val lastIndex = list?.lastIndex ?: -1

    fun isInRange(index: Int) = list?.let { index in it.indices } ?: false

    companion object {
        fun empty() = SplicingTableClientView(
            list = null,
            clipboard = null,
            isListWritable = false,
            isClipboardWritable = false,
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
    constructor(iota: Iota, level: ServerLevel) : this(
        tag = HexIotaTypes.serialize(iota),
        name = iota.displayWithPatternName(level),
        hexpatternSource = iota.toHexpatternSource(level),
        pattern = (iota as? PatternIota)?.pattern,
    )
}

fun List<IotaClientView>.toHexpatternSource(): String {
    var depth = 0
    return joinToString("\n") {
        if (it.pattern == SpecialPatterns.RETROSPECTION) depth--
        val indent = " ".repeat(max(0, 4 * depth))
        if (it.pattern == SpecialPatterns.INTROSPECTION) depth++
        indent + it.hexpatternSource
    }
}
