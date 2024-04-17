package ca.objectobject.hexdebug;

import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.function.Consumer;

public interface IHexDebugAbstractions {
    Path getConfigDirectory();

    void initPlatformSpecific();

    void onServerStarted(Consumer<MinecraftServer> callback);

    void onServerStopping(Consumer<MinecraftServer> callback);
}
