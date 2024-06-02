package gay.`object`.hexdebug.splicing

import net.minecraft.nbt.CompoundTag

data class SplicingTableClientView(
    val list: List<CompoundTag>?,
    val clipboard: CompoundTag?,
    val isListWritable: Boolean,
    val isClipboardWritable: Boolean,
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
        )
    }
}
