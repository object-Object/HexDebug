package gay.object.hexdebug.api;

import gay.object.hexdebug.HexDebug;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

@SuppressWarnings("SameParameterValue")
public final class HexDebugTags {
    public static final class Items {
        public static final TagKey<Item> SPLICING_TABLE_MEDIA_BLACKLIST = create("splicing_table/media_blacklist");

        private static TagKey<Item> create(String name) {
            return TagKey.create(Registries.ITEM, HexDebug.id(name));
        }
    }
}
