package gay.`object`.hexdebug.forge.datagen

import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.common.lib.HexBlocks
import at.petrak.hexcasting.common.lib.HexItems
import at.petrak.paucal.api.datagen.PaucalRecipeProvider
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.registry.HexDebugBlocks
import gay.`object`.hexdebug.registry.HexDebugItems
import net.minecraft.data.PackOutput
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Items
import net.minecraft.world.level.ItemLike
import java.util.function.Consumer

class HexDebugRecipes(output: PackOutput) : PaucalRecipeProvider(output, HexDebug.MODID) {
    override fun buildRecipes(writer: Consumer<FinishedRecipe>) {
        // debugger
        flyswatter(HexDebugItems.DEBUGGER.value, Items.GOLD_INGOT, HexItems.ARTIFACT)
            .unlockedBy("has_item", hasItem(HexTags.Items.STAVES))
            .save(writer)

        // evaluator
        flyswatter(HexDebugItems.EVALUATOR.value, HexBlocks.SLATE_BLOCK)
            .unlockedBy("has_item", hasItem(HexTags.Items.STAVES))
            .save(writer)

        // splicing table
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, HexDebugBlocks.SPLICING_TABLE.value)
            .define('P', HexBlocks.EDIFIED_PLANKS)
            .define('C', HexItems.CHARGED_AMETHYST)
            .define('A', Items.AMETHYST_SHARD)
            .define('F', HexItems.FOCUS)
            .define('S', HexBlocks.SLATE_BLOCK)
            .define('G', Items.GOLD_INGOT)
            .pattern("PCP")
            .pattern("AFA")
            .pattern("SGS")
            .unlockedBy("has_item", hasItem(HexItems.FOCUS))
            .save(writer)

        // empty focal frame
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, HexDebugBlocks.FOCUS_HOLDER.value)
            .define('S', HexBlocks.SLATE_BLOCK)
            .define('G', Items.GOLD_NUGGET)
            .pattern("GSG")
            .pattern("S S")
            .pattern("GSG")
            .unlockedBy("has_item", hasItem(HexItems.FOCUS))
            .save(writer)
    }

    @Suppress("SameParameterValue")
    private fun flyswatter(result: ItemLike, handle: ItemLike) = flyswatter(result, handle, handle)

    // thwack!
    private fun flyswatter(result: ItemLike, lowerHandle: ItemLike, upperHandle: ItemLike) =
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, result)
            .define('L', lowerHandle)
            .define('U', upperHandle)
            .define('C', HexItems.CHARGED_AMETHYST)
            .pattern(" CC")
            .pattern(" UC")
            .pattern("L  ")
}
