package gay.`object`.hexdebug.datagen.recipes

import com.google.gson.JsonObject
import gay.`object`.hexdebug.registry.HexDebugBlocks
import gay.`object`.hexdebug.registry.HexDebugRecipeSerializers
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.ItemLike
import java.util.function.Consumer

class FocusHolderFillingShapedRecipeBuilder(
    category: RecipeCategory,
    result: ItemLike,
    count: Int,
) : ShapedRecipeBuilder(category, result, count) {
    override fun save(finishedRecipeConsumer: Consumer<FinishedRecipe>, recipeId: ResourceLocation) {
        if (result !== HexDebugBlocks.FOCUS_HOLDER.item) {
            throw IllegalStateException("Expected result item to be ${HexDebugBlocks.FOCUS_HOLDER.id} but got $result")
        }
        super.save({ finishedRecipeConsumer.accept(Result(it)) }, recipeId)
    }

    companion object {
        fun shaped(category: RecipeCategory, result: ItemLike): FocusHolderFillingShapedRecipeBuilder =
            shaped(category, result, 1)

        fun shaped(category: RecipeCategory, result: ItemLike, count: Int): FocusHolderFillingShapedRecipeBuilder =
            FocusHolderFillingShapedRecipeBuilder(category, result, count)
    }

    private class Result(private val inner: FinishedRecipe) : FinishedRecipe {
        override fun getType() = HexDebugRecipeSerializers.FOCUS_HOLDER_FILLING_SHAPED.value

        override fun getId(): ResourceLocation = inner.id

        override fun getAdvancementId() = inner.advancementId

        override fun serializeRecipeData(json: JsonObject) {
            inner.serializeRecipeData(json)
        }

        override fun serializeAdvancement(): JsonObject? {
            return inner.serializeAdvancement()
        }
    }
}
