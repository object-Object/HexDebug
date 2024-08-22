package gay.`object`.hexdebug.splicing

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import gay.`object`.hexdebug.utils.displayWithPatternName
import gay.`object`.hexdebug.utils.toHexpatternSource
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component

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
) {
    constructor(iota: Iota, env: CastingEnvironment) : this(
        tag = IotaType.serialize(iota),
        name = iota.displayWithPatternName(env),
        hexpatternSource = iota.toHexpatternSource(env),
    )
}
