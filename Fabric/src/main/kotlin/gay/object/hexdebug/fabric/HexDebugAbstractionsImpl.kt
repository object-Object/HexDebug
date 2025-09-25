@file:JvmName("HexDebugAbstractionsImpl")

package gay.`object`.hexdebug.fabric

import gay.`object`.hexdebug.registry.HexDebugRegistrar
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

fun <T : Any> initRegistry(registrar: HexDebugRegistrar<T>) {
    val registry = registrar.registry
    registrar.init { id, value -> Registry.register(registry, id, value) }
}

fun registerClientResourceReloadListener(id: ResourceLocation, listener: PreparableReloadListener) {
    ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
        object : IdentifiableResourceReloadListener {
            override fun reload(
                preparationBarrier: PreparableReloadListener.PreparationBarrier,
                resourceManager: ResourceManager,
                preparationsProfiler: ProfilerFiller,
                reloadProfiler: ProfilerFiller,
                backgroundExecutor: Executor,
                gameExecutor: Executor
            ): CompletableFuture<Void> {
                return listener.reload(preparationBarrier, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor)
            }

            override fun getFabricId() = id
        }
    )
}
