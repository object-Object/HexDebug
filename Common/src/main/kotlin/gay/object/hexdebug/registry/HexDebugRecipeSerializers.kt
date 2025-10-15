package gay.`object`.hexdebug.registry

import gay.`object`.hexdebug.recipes.FlyswatterQuenchingShapedRecipe
import gay.`object`.hexdebug.recipes.FocusHolderFillingShapedRecipe
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.crafting.RecipeSerializer

object HexDebugRecipeSerializers : HexDebugRegistrar<RecipeSerializer<*>>(
    Registries.RECIPE_SERIALIZER,
    { BuiltInRegistries.RECIPE_SERIALIZER },
) {
    val FLYSWATTER_QUENCHING = register("flyswatter_quenching") {
        FlyswatterQuenchingShapedRecipe.Serializer()
    }

    val FOCUS_HOLDER_FILLING_SHAPED = register("focus_holder_filling_shaped") {
        FocusHolderFillingShapedRecipe.Serializer()
    }
}
