package ca.objectobject.hexdebug.registry;

import ca.objectobject.hexdebug.HexDebug;
import ca.objectobject.hexdebug.common.items.ItemDebugger;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public class HexDebugItemRegistry {
    // Register items through this
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(HexDebug.MODID, Registries.ITEM);

    public static void init() {
        ITEMS.register();
    }

    // During the loading phase, refrain from accessing suppliers' items (e.g. EXAMPLE_ITEM.get()), they will not be available
    public static final RegistrySupplier<Item> DUMMY_ITEM = ITEMS.register("debugger", () -> new ItemDebugger(new ItemDebugger.Properties()));


}