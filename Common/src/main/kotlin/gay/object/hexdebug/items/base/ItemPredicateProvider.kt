package gay.`object`.hexdebug.items.base

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

typealias ModelPredicate = (stack: ItemStack, level: ClientLevel?, entity: LivingEntity?, i: Int) -> Float

interface ItemPredicateProvider {
    fun getModelPredicates(): Iterable<ModelPredicateEntry>
}

data class ModelPredicateEntry(
    val id: ResourceLocation,
    val predicate: ClampedItemPropertyFunction,
) {
    constructor(id: ResourceLocation, predicate: ModelPredicate) : this(id, ClampedItemPropertyFunction(predicate))
}
