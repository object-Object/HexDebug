package gay.object.hexdebug.fabric;

import gay.object.hexdebug.IHexDebugAbstractions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.function.Consumer;

public class HexDebugAbstractionsImpl implements IHexDebugAbstractions {
    public static IHexDebugAbstractions get() {
        return new HexDebugAbstractionsImpl();
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void initPlatformSpecific() {
        HexDebugConfigFabric.init();
    }

    @Override
    public void onServerStarted(Consumer<MinecraftServer> callback) {
        ServerLifecycleEvents.SERVER_STARTED.register(callback::accept);
    }

    @Override
    public void onServerStopping(Consumer<MinecraftServer> callback) {
        ServerLifecycleEvents.SERVER_STOPPING.register(callback::accept);
    }
}
