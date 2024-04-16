package ca.objectobject.hexdebug.fabric;

import net.fabricmc.api.ClientModInitializer;
import ca.objectobject.hexdebug.HexDebugClient;

/**
 * Fabric client loading entrypoint.
 */
public class HexDebugClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HexDebugClient.init();
    }
}
