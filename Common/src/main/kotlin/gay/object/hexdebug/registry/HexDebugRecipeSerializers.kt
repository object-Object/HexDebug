package gay.`object`.hexdebug.registry

import gay.`object`.hexdebug.recipes.FocusHolderEmptyingRecipe
import gay.`object`.hexdebug.recipes.FocusHolderFillingShapedRecipe
import gay.`object`.hexdebug.recipes.FocusHolderFillingShapelessRecipe
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer

object HexDebugRecipeSerializers : HexDebugRegistrar<RecipeSerializer<*>>(
    Registries.RECIPE_SERIALIZER,
    { BuiltInRegistries.RECIPE_SERIALIZER },
) {
    val FOCUS_HOLDER_EMPTYING = register("focus_holder_emptying") {
        SimpleCraftingRecipeSerializer(::FocusHolderEmptyingRecipe)
    }

    val FOCUS_HOLDER_FILLING_SHAPED = register("focus_holder_filling_shaped") {
        FocusHolderFillingShapedRecipe.Serializer()
    }

    val FOCUS_HOLDER_FILLING_SHAPELESS = register("focus_holder_filling_shapeless") {
        SimpleCraftingRecipeSerializer(::FocusHolderFillingShapelessRecipe)
    }
}
