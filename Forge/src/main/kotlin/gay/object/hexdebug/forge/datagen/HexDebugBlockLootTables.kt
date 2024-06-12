package gay.`object`.hexdebug.forge.datagen

import gay.`object`.hexdebug.blocks.focusholder.FocusHolderBlock
import gay.`object`.hexdebug.registry.HexDebugBlockEntities
import gay.`object`.hexdebug.registry.HexDebugBlocks
import net.minecraft.data.loot.BlockLootSubProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.entries.DynamicLoot
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.functions.SetContainerContents
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue

class HexDebugBlockLootTables : BlockLootSubProvider(setOf(), FeatureFlags.DEFAULT_FLAGS) {
    override fun generate() {
        dropSelf(HexDebugBlocks.SPLICING_TABLE.value)

        add(HexDebugBlocks.FOCUS_HOLDER.value) { block ->
            dropWithContents(block, HexDebugBlockEntities.FOCUS_HOLDER.value, FocusHolderBlock.CONTENTS)
        }
    }

    override fun getKnownBlocks(): MutableIterable<Block> {
        return HexDebugBlocks.entries.map { it.value.value }.toMutableList()
    }

    // https://discord.com/channels/313125603924639766/915304642668290119/960548805802086460 (NeoForge Discord)
    private fun dropWithContents(
        block: ItemLike,
        blockEntityType: BlockEntityType<*>,
        dynamicDropsName: ResourceLocation,
    ) = LootTable.lootTable().withPool(
        applyExplosionCondition(
            block,
            LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1f))
                .add(LootItem.lootTableItem(block))
                .apply(
                    SetContainerContents
                        .setContents(blockEntityType)
                        .withEntry(DynamicLoot.dynamicEntry(dynamicDropsName))
                )

        )
    )
}
