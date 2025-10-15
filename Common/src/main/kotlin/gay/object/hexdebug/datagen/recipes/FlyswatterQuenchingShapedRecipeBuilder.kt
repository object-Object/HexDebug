package gay.`object`.hexdebug.datagen.recipes

import com.google.gson.JsonObject
import gay.`object`.hexdebug.registry.HexDebugRecipeSerializers
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.ItemLike
import java.util.function.Consumer

class FlyswatterQuenchingShapedRecipeBuilder(
    category: RecipeCategory,
    result: ItemLike,
    count: Int = 1,
) : ShapedRecipeBuilder(category, result, count) {
    override fun save(finishedRecipeConsumer: Consumer<FinishedRecipe>, recipeId: ResourceLocation) {
        super.save({ finishedRecipeConsumer.accept(Result(it)) }, recipeId)
    }

    private class Result(private val inner: FinishedRecipe) : FinishedRecipe {
        override fun getType() = HexDebugRecipeSerializers.FLYSWATTER_QUENCHING.value

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
