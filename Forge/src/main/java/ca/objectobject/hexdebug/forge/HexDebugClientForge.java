package ca.objectobject.hexdebug.forge;

import ca.objectobject.hexdebug.HexDebugClient;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Forge client loading entrypoint.
 */
public class HexDebugClientForge {
    public static void init(FMLClientSetupEvent event) {
        HexDebugClient.init();
    }
}
