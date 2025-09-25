package gay.object.hexdebug.api.client.splicing;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface SplicingTableIotaRendererParser<T extends SplicingTableIotaRendererProvider> {
    /**
     * Attempts to parse the JSON data and return a provider for an iota renderer.
     * Throws {@link IllegalArgumentException} or {@link com.google.gson.JsonParseException} if
     * the input is invalid.
     */
    @NotNull
    T parse(@NotNull Gson gson, @NotNull JsonObject jsonObject, @Nullable T parent);

    /**
     * Creates a parser that always returns the given instance.
     */
    @NotNull
    static <T extends SplicingTableIotaRendererProvider> SplicingTableIotaRendererParser<T> of(T instance) {
        return (gson, jsonObject, parent) -> instance;
    }
}
