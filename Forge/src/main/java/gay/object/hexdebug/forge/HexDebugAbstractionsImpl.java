package gay.object.hexdebug.forge;

import gay.object.hexdebug.IHexDebugAbstractions;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.function.Consumer;

public class HexDebugAbstractionsImpl implements IHexDebugAbstractions {
    public static IHexDebugAbstractions get() {
        return new HexDebugAbstractionsImpl();
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public void initPlatformSpecific() {
        HexDebugConfigForge.init();
    }

    @Override
    public void onServerStarted(Consumer<MinecraftServer> callback) {
        // TODO: is this how you do this???
        MinecraftForge.EVENT_BUS.addListener((Consumer<ServerStartedEvent>) event -> {
            callback.accept(event.getServer());
        });
    }

    @Override
    public void onServerStopping(Consumer<MinecraftServer> callback) {
        MinecraftForge.EVENT_BUS.addListener((Consumer<ServerStoppingEvent>) event -> {
            callback.accept(event.getServer());
        });
    }
}
