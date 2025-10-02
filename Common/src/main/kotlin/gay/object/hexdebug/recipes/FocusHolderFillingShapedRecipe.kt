package gay.`object`.hexdebug.recipes

import com.google.gson.JsonObject
import gay.`object`.hexdebug.items.FocusHolderBlockItem.Companion.hasIotaStack
import gay.`object`.hexdebug.items.FocusHolderBlockItem.Companion.setIotaStack
import gay.`object`.hexdebug.registry.HexDebugBlocks
import gay.`object`.hexdebug.registry.HexDebugRecipeSerializers
import net.minecraft.core.NonNullList
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.ShapedRecipe
import net.minecraft.world.level.Level

class FocusHolderFillingShapedRecipe(
    id: ResourceLocation,
    group: String,
    category: CraftingBookCategory,
    width: Int,
    height: Int,
    recipeItems: NonNullList<Ingredient>,
    result: ItemStack,
    val resultInner: ItemStack,
    showNotification: Boolean,
) : ShapedRecipe(id, group, category, width, height, recipeItems, result, showNotification) {
    override fun matches(container: CraftingContainer, level: Level): Boolean {
        if (!super.matches(container, level)) return false
        for (ingredient in container.items) {
            // don't allow filling a holder that's already filled
            if (ingredient.`is`(HexDebugBlocks.FOCUS_HOLDER.item) && ingredient.hasIotaStack) {
                return false
            }
        }
        return true
    }

    override fun getSerializer() = HexDebugRecipeSerializers.FOCUS_HOLDER_FILLING_SHAPED.value

    companion object {
        private fun fromShapedRecipe(recipe: ShapedRecipe, resultInner: ItemStack): FocusHolderFillingShapedRecipe {
            return recipe.run {
                FocusHolderFillingShapedRecipe(
                    id = id,
                    group = group,
                    category = category(),
                    width = width,
                    height = height,
                    recipeItems = ingredients,
                    result = ItemStack(HexDebugBlocks.FOCUS_HOLDER.item).setIotaStack(resultInner),
                    resultInner = resultInner,
                    showNotification = showNotification(),
                )
            }
        }
    }

    class Serializer : ShapedRecipe.Serializer() {
        override fun fromJson(recipeId: ResourceLocation, json: JsonObject): ShapedRecipe {
            val recipe = super.fromJson(recipeId, json)
            val resultInner = itemStackFromJson(GsonHelper.getAsJsonObject(json, "result_inner"))
            return fromShapedRecipe(recipe, resultInner)
        }

        override fun fromNetwork(recipeId: ResourceLocation, buf: FriendlyByteBuf): ShapedRecipe {
            val recipe = super.fromNetwork(recipeId, buf)
            val resultInner = buf.readItem()
            return fromShapedRecipe(recipe, resultInner)
        }

        override fun toNetwork(buf: FriendlyByteBuf, recipe: ShapedRecipe) {
            super.toNetwork(buf, recipe)
            buf.writeItem((recipe as FocusHolderFillingShapedRecipe).resultInner)
        }
    }
}
