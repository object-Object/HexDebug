package ca.objectobject.hexdebug.forge;

import dev.architectury.platform.forge.EventBuses;
import ca.objectobject.hexdebug.HexDebug;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * This is your loading entrypoint on forge, in case you need to initialize
 * something platform-specific.
 */
@Mod(HexDebug.MOD_ID)
public class HexDebugForge {
    public HexDebugForge() {
        // Submit our event bus to let architectury register our content on the right time
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(HexDebug.MOD_ID, bus);
        bus.addListener(HexDebugClientForge::init);
        HexDebug.init();
    }
}
