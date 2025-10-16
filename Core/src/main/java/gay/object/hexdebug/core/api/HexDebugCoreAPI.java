package gay.object.hexdebug.core.api;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import gay.object.hexdebug.core.api.debugging.DebugEnvironment;
import gay.object.hexdebug.core.api.debugging.DebugOutputCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ServiceLoader;
import java.util.UUID;
import java.util.stream.Collectors;

public interface HexDebugCoreAPI {
    @Nullable
    DebugEnvironment getDebugEnv(@NotNull CastingEnvironment env);

    void printDebugMessage(
        @NotNull ServerPlayer caster,
        @NotNull UUID sessionId,
        @NotNull Component message,
        @NotNull DebugOutputCategory category,
        boolean withSource
    );

    HexDebugCoreAPI INSTANCE = findInstance();

    private static HexDebugCoreAPI findInstance() {
        var providers = ServiceLoader.load(HexDebugCoreAPI.class).stream().toList();
        if (providers.size() > 1) {
            var names = providers.stream()
                .map(p -> p.type().getName())
                .collect(Collectors.joining(",", "[", "]"));
            throw new IllegalStateException(
                "Expected at most one HexDebugCoreAPI implementation on the classpath. Found: " + names
            );
        } else if (providers.size() == 1) {
            return providers.get(0).get();
        } else {
            return new HexDebugCoreAPIStubImpl();
        }
    }
}
