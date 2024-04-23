package gay.object.hexdebug.fabric;

import gay.object.hexdebug.IHexDebugAbstractions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

import java.util.function.Consumer;

public class HexDebugAbstractionsImpl implements IHexDebugAbstractions {
    public static IHexDebugAbstractions get() {
        return new HexDebugAbstractionsImpl();
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

    @Override
    public void onClientJoin(Consumer<Minecraft> callback) {
        ClientPlayConnectionEvents.JOIN.register((l, s, client) -> callback.accept(client));
    }

    @Override
    public void onClientDisconnect(Consumer<Minecraft> callback) {
        ClientPlayConnectionEvents.DISCONNECT.register((l, client) -> callback.accept(client));
    }
}
