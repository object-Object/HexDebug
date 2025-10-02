package gay.`object`.hexdebug.fabric

import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.HexDebugClient
import gay.`object`.hexdebug.resources.splicing.SplicingTableIotasResourceReloadListener
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

object FabricHexDebugClient : ClientModInitializer {
    override fun onInitializeClient() {
        HexDebugClient.init()

        registerClientResourceListener(
            HexDebug.id("hexdebug_splicing_iotas"),
            SplicingTableIotasResourceReloadListener,
        )
    }
}

private fun registerClientResourceListener(id: ResourceLocation, listener: PreparableReloadListener) {
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
