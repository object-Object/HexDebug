package gay.object.hexdebug.forge;

import gay.object.hexdebug.HexDebugClient;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Forge client loading entrypoint.
 */
public class HexDebugClientForge {
    public static void init(FMLClientSetupEvent event) {
        HexDebugClient.init();
    }
}
