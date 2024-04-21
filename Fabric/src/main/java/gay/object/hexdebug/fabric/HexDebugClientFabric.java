package gay.object.hexdebug.fabric;

import net.fabricmc.api.ClientModInitializer;
import gay.object.hexdebug.HexDebugClient;

/**
 * Fabric client loading entrypoint.
 */
public class HexDebugClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HexDebugClient.init();
    }
}
