package gay.object.hexdebug;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

import java.util.function.Consumer;

public interface IHexDebugAbstractions {
    void initPlatformSpecific();

    void onServerStarted(Consumer<MinecraftServer> callback);

    void onServerStopping(Consumer<MinecraftServer> callback);

    void onClientJoin(Consumer<Minecraft> callback);

    void onClientDisconnect(Consumer<Minecraft> callback);
}
