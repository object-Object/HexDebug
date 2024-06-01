package gay.`object`.hexdebug.splicing

import net.minecraft.nbt.CompoundTag

data class SplicingTableClientView(
    val iotas: List<CompoundTag>?,
    val clipboard: CompoundTag?,
    val isWritable: Boolean,
    val isClipboardWritable: Boolean,
) {
    val hasIotas = iotas != null
    val hasClipboard = null != clipboard // thanks kotlin

    val lastIotaIndex = iotas?.lastIndex ?: -1

    fun isInRange(index: Int) = iotas?.let { index in it.indices } ?: false

    companion object {
        fun empty() = SplicingTableClientView(
            iotas = null,
            clipboard = null,
            isWritable = false,
            isClipboardWritable = false,
        )
    }
}
