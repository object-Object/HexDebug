package gay.`object`.hexdebug.splicing

import gay.`object`.hexdebug.api.splicing.SplicingTableIotaClientView
import net.minecraft.nbt.CompoundTag

data class SplicingTableClientView(
    val list: List<SplicingTableIotaClientView>?,
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
