package ca.objectobject.hexdebug.registry;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import ca.objectobject.hexdebug.HexDebug;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Registry;

import static ca.objectobject.hexdebug.HexDebug.id;

public class HexDebugItemRegistry {
    // Register items through this
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(HexDebug.MOD_ID, Registry.ITEM_REGISTRY);

    public static void init() {
        ITEMS.register();
    }

    // A new creative tab. Notice how it is one of the few things that are not deferred
    public static final CreativeModeTab DUMMY_GROUP = CreativeTabRegistry.create(id("dummy_group"), () -> new ItemStack(HexDebugItemRegistry.DUMMY_ITEM.get()));

    // During the loading phase, refrain from accessing suppliers' items (e.g. EXAMPLE_ITEM.get()), they will not be available
    public static final RegistrySupplier<Item> DUMMY_ITEM = ITEMS.register("dummy_item", () -> new Item(new Item.Properties().tab(DUMMY_GROUP)));


}
