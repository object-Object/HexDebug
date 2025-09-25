package gay.object.hexdebug.api.client.splicing;

import com.google.common.collect.Maps;
import gay.object.hexdebug.HexDebug;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class SplicingTableIotaRenderers {
    private static final Map<ResourceLocation, SplicingTableIotaRendererParser<?>> PARSERS = Maps.newHashMap();

    /**
     * Register a splicing table iota renderer.
     * <br>
     * This is used to parse resource files and create providers for iota renderers.
     */
    public static void register(
        @NotNull ResourceLocation id,
        @NotNull SplicingTableIotaRendererParser<?> parser
    ) {
        if (PARSERS.containsKey(id)) {
            HexDebug.LOGGER.warn("Overriding existing splicing table iota renderer parser: {}", id);
        }
        PARSERS.put(id, parser);
    }

    @ApiStatus.Internal
    @Nullable
    public static SplicingTableIotaRendererParser<?> getParser(@NotNull ResourceLocation id) {
        return PARSERS.get(id);
    }
}
