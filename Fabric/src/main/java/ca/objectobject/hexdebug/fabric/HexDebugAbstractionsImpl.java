package ca.objectobject.hexdebug.fabric;

import net.fabricmc.loader.api.FabricLoader;
import ca.objectobject.hexdebug.HexDebugAbstractions;

import java.nio.file.Path;

public class HexDebugAbstractionsImpl {
    /**
     * This is the actual implementation of {@link HexDebugAbstractions#getConfigDirectory()}.
     */
    public static Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }
	
    public static void initPlatformSpecific() {
        HexDebugConfigFabric.init();
    }
}
