package gay.`object`.hexdebug.forge.datagen

import gay.`object`.hexdebug.registry.HexDebugBlocks
import net.minecraft.data.loot.BlockLootSubProvider
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.level.block.Block

class HexDebugBlockLootTables : BlockLootSubProvider(setOf(), FeatureFlags.DEFAULT_FLAGS) {
    override fun generate() {
        dropSelf(HexDebugBlocks.SPLICING_TABLE.value)
        dropSelf(HexDebugBlocks.ENLIGHTENED_SPLICING_TABLE.value)
        dropSelf(HexDebugBlocks.FOCUS_HOLDER.value)
    }

    override fun getKnownBlocks(): MutableIterable<Block> {
        return HexDebugBlocks.entries.map { it.value }.toMutableList()
    }
}
