package gay.`object`.hexdebug.forge.datagen

import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.common.lib.HexBlocks
import at.petrak.hexcasting.common.lib.HexItems
import at.petrak.hexcasting.common.recipe.ingredient.StateIngredientHelper
import at.petrak.hexcasting.common.recipe.ingredient.brainsweep.VillagerIngredient
import at.petrak.hexcasting.datagen.HexAdvancements
import at.petrak.hexcasting.datagen.recipe.builders.BrainsweepRecipeBuilder
import at.petrak.paucal.api.datagen.PaucalRecipeProvider
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.datagen.recipes.FocusHolderFillingShapedRecipeBuilder
import gay.`object`.hexdebug.registry.HexDebugBlocks
import gay.`object`.hexdebug.registry.HexDebugItems
import net.minecraft.data.PackOutput
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.entity.npc.VillagerProfession
import net.minecraft.world.item.Items
import net.minecraft.world.level.ItemLike
import java.util.function.Consumer

class HexDebugRecipes(output: PackOutput) : PaucalRecipeProvider(output, HexDebug.MODID) {
    override fun buildRecipes(writer: Consumer<FinishedRecipe>) {
        // debugger
        flyswatter(HexDebugItems.DEBUGGER, Items.GOLD_INGOT, HexItems.ARTIFACT)
            .unlockedBy("has_item", hasItem(HexTags.Items.STAVES))
            .save(writer)

        // evaluator
        flyswatter(HexDebugItems.EVALUATOR, HexBlocks.SLATE_BLOCK)
            .unlockedBy("has_item", hasItem(HexTags.Items.STAVES))
            .save(writer)

        // splicing table
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, HexDebugBlocks.SPLICING_TABLE)
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

        // enlightened splicing table
        BrainsweepRecipeBuilder(
            StateIngredientHelper.of(HexDebugBlocks.SPLICING_TABLE.block),
            VillagerIngredient(VillagerProfession.TOOLSMITH, null, 3),
            HexDebugBlocks.ENLIGHTENED_SPLICING_TABLE.block.defaultBlockState(),
            MediaConstants.CRYSTAL_UNIT * 10,
        )
            .unlockedBy("enlightenment", HexAdvancements.ENLIGHTEN)
            .save(writer, HexDebug.id("brainsweep/enlightened_splicing_table"))

        // empty focus holder
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, HexDebugBlocks.FOCUS_HOLDER)
            .define('S', HexBlocks.SLATE_BLOCK)
            .define('G', Items.GOLD_NUGGET)
            .pattern("GSG")
            .pattern("S S")
            .pattern("GSG")
            .unlockedBy("has_item", hasItem(HexItems.FOCUS))
            .save(writer)

        // existing focus holder with new focus
        FocusHolderFillingShapedRecipeBuilder(RecipeCategory.MISC, HexDebugBlocks.FOCUS_HOLDER, HexItems.FOCUS)
            .define('G', Items.GLOWSTONE)
            .define('L', Items.LEATHER)
            .define('P', Items.PAPER)
            .define('A', HexItems.CHARGED_AMETHYST)
            .define('H', HexDebugBlocks.FOCUS_HOLDER)
            .pattern("HL ")
            .pattern("PAP")
            .pattern(" LG")
            .unlockedBy("has_item", hasItem(HexItems.FOCUS))
            .save(writer, HexDebug.id("focus_holder_filling_shaped/focus"))
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
