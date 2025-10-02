package gay.object.hexdebug.api.client.splicing;

import at.petrak.hexcasting.api.casting.iota.IotaType;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import gay.object.hexdebug.HexDebug;
import gay.object.hexdebug.resources.splicing.SplicingTableIotasResourceReloadListener;
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
        @NotNull ResourceLocation parserId,
        @NotNull SplicingTableIotaRendererParser<?> parser
    ) {
        if (PARSERS.containsKey(parserId)) {
            HexDebug.LOGGER.warn("Overriding existing splicing table iota renderer parser: {}", parserId);
        }
        PARSERS.put(parserId, parser);
    }

    /**
     * Get/load a provider by ID. Throws if the referenced provider does not exist or fails to load.
     * Intended for use in providers that reference other providers.
     * <br>
     * This should only be called from inside of {@link SplicingTableIotaRendererParser#parse}.
     */
    @NotNull
    public static SplicingTableIotaRendererProvider loadProvider(@NotNull ResourceLocation providerId) {
        return SplicingTableIotasResourceReloadListener.loadProvider(providerId);
    }

    /**
     * Parse a provider from a raw JSON object. Intended for use in providers that contain other
     * providers.
     * <br>
     * This should only be called from inside of {@link SplicingTableIotaRendererParser#parse}.
     */
    @NotNull
    public static SplicingTableIotaRendererProvider parseProvider(@NotNull JsonObject jsonObject) {
        return SplicingTableIotasResourceReloadListener.parseProvider(jsonObject);
    }

    /**
     * Get a previously loaded provider for a given iota type.
     * <br>
     * Returns the fallback provider if the type is {@code null} or no provider was found for that
     * type, or {@code null} if the fallback provider has not yet been loaded.
     */
    @Nullable
    public static SplicingTableIotaRendererProvider getProvider(@Nullable IotaType<?> iotaType) {
        return getProvider(iotaType, true);
    }

    /**
     * Get a previously loaded provider for a given iota type.
     * <br>
     * Returns the fallback provider if {@code useFallback} is {@code true} and either the type is
     * {@code null} or no provider was found for that type, or {@code null} if {@code useFallback}
     * is {@code false} or the fallback provider has not yet been loaded.
     */
    @Nullable
    public static SplicingTableIotaRendererProvider getProvider(@Nullable IotaType<?> iotaType, boolean useFallback) {
        return SplicingTableIotasResourceReloadListener.getProvider(iotaType, useFallback);
    }

    @ApiStatus.Internal
    @Nullable
    public static SplicingTableIotaRendererParser<?> getParser(@NotNull ResourceLocation parserId) {
        return PARSERS.get(parserId);
    }
}
