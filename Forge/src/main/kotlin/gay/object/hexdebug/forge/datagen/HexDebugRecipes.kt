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
import gay.`object`.hexdebug.registry.HexDebugRecipeSerializers
import gay.`object`.hexdebug.registry.RegistrarEntry
import net.minecraft.data.PackOutput
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.data.recipes.SpecialRecipeBuilder
import net.minecraft.world.entity.npc.VillagerProfession
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.RecipeSerializer
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

        // enlightened splicing table
        BrainsweepRecipeBuilder(
            StateIngredientHelper.of(HexDebugBlocks.SPLICING_TABLE.value),
            VillagerIngredient(VillagerProfession.TOOLSMITH, null, 2),
            HexDebugBlocks.ENLIGHTENED_SPLICING_TABLE.block.defaultBlockState(),
            MediaConstants.CRYSTAL_UNIT * 10,
        )
            .unlockedBy("enlightenment", HexAdvancements.ENLIGHTEN)
            .save(writer, HexDebug.id("brainsweep/enlightened_splicing_table"))

        // empty focus holder
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, HexDebugBlocks.FOCUS_HOLDER.value)
            .define('S', HexBlocks.SLATE_BLOCK)
            .define('G', Items.GOLD_NUGGET)
            .pattern("GSG")
            .pattern("S S")
            .pattern("GSG")
            .unlockedBy("has_item", hasItem(HexItems.FOCUS))
            .save(writer)

        // new focus holder with existing focus
        FocusHolderFillingShapedRecipeBuilder.shaped(RecipeCategory.MISC, HexDebugBlocks.FOCUS_HOLDER.value)
            .define('S', HexBlocks.SLATE_BLOCK)
            .define('G', Items.GOLD_NUGGET)
            .define('F', HexItems.FOCUS)
            .pattern("GSG")
            .pattern("SFS")
            .pattern("GSG")
            .unlockedBy("has_item", hasItem(HexItems.FOCUS))
            .save(writer, HexDebug.id("focus_holder_filling_shaped/new_holder"))

        // existing focus holder with new focus
        FocusHolderFillingShapedRecipeBuilder.shaped(RecipeCategory.MISC, HexDebugBlocks.FOCUS_HOLDER.value)
            .define('G', Items.GLOWSTONE)
            .define('L', Items.LEATHER)
            .define('P', Items.PAPER)
            .define('A', HexItems.CHARGED_AMETHYST)
            .define('H', HexDebugBlocks.FOCUS_HOLDER.value)
            .pattern("HL ")
            .pattern("PAP")
            .pattern(" LG")
            .unlockedBy("has_item", hasItem(HexItems.FOCUS))
            .save(writer, HexDebug.id("focus_holder_filling_shaped/new_focus"))

        // existing focus holder with existing focus
        specialRecipe(writer, HexDebugRecipeSerializers.FOCUS_HOLDER_FILLING_SHAPELESS)

        // remove focus from holder
        specialRecipe(writer, HexDebugRecipeSerializers.FOCUS_HOLDER_EMPTYING)
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

    private fun specialRecipe(writer: Consumer<FinishedRecipe>, entry: RegistrarEntry<RecipeSerializer<*>>) {
        SpecialRecipeBuilder(entry.value).save(writer, entry.id.toString())
    }
}
