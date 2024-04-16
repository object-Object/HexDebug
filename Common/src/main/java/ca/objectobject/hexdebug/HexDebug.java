package ca.objectobject.hexdebug;

import ca.objectobject.hexdebug.registry.HexDebugIotaTypeRegistry;
import ca.objectobject.hexdebug.registry.HexDebugItemRegistry;
import ca.objectobject.hexdebug.registry.HexDebugPatternRegistry;
import ca.objectobject.hexdebug.networking.HexDebugNetworking;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This is effectively the loading entrypoint for most of your code, at least
 * if you are using Architectury as intended.
 */
public class HexDebug {
    public static final String MOD_ID = "hexdebug";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);


    public static void init() {
        LOGGER.info("HexDebug says hello!");

        HexDebugAbstractions.initPlatformSpecific();
        HexDebugItemRegistry.init();
        HexDebugIotaTypeRegistry.init();
        HexDebugPatternRegistry.init();
		HexDebugNetworking.init();

        LOGGER.info(HexDebugAbstractions.getConfigDirectory().toAbsolutePath().normalize().toString());
    }

    /**
     * Shortcut for identifiers specific to this mod.
     */
    public static ResourceLocation id(String string) {
        return new ResourceLocation(MOD_ID, string);
    }
}
