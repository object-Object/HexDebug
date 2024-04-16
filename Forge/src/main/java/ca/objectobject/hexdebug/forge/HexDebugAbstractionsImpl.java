package ca.objectobject.hexdebug.forge;

import ca.objectobject.hexdebug.HexDebugAbstractions;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class HexDebugAbstractionsImpl {
    /**
     * This is the actual implementation of {@link HexDebugAbstractions#getConfigDirectory()}.
     */
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
	
    public static void initPlatformSpecific() {
        HexDebugConfigForge.init();
    }
}
