package gay.object.hexdebug.api.client.splicing;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface SplicingTableIotaRendererParser<T extends SplicingTableIotaRendererProvider> {
    /**
     * Attempts to parse the JSON data and return a provider for an iota renderer.
     * Throws {@link IllegalArgumentException} or {@link com.google.gson.JsonParseException} if
     * the input is invalid.
     */
    @ApiStatus.OverrideOnly
    @NotNull
    T parse(@NotNull Gson gson, @NotNull JsonObject jsonObject, @Nullable T parent);

    /** Creates a parser that always returns the given provider. */
    @NotNull
    static SplicingTableIotaRendererParser<?> simple(SplicingTableIotaRendererProvider provider) {
        return (gson, jsonObject, parent) -> provider;
    }
}
