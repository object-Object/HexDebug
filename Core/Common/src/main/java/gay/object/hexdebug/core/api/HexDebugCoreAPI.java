package gay.object.hexdebug.core.api;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.iota.Iota;
import gay.object.hexdebug.core.api.debugging.env.DebugEnvironment;
import gay.object.hexdebug.core.api.debugging.DebugOutputCategory;
import gay.object.hexdebug.core.api.exceptions.IllegalDebugSessionException;
import gay.object.hexdebug.core.api.exceptions.IllegalDebugThreadException;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Main API service for hexdebug-core. Access via {@link HexDebugCoreAPI#INSTANCE}.
 * <br>
 * Unless otherwise stated, all API methods must be called only from the main server thread.
 */
public interface HexDebugCoreAPI {
    // required methods

    @Contract(pure = true)
    @Nullable
    default DebugEnvironment getDebugEnv(@NotNull CastingEnvironment env) {
        return null;
    }

    @Contract(pure = true)
    @Nullable
    default DebugEnvironment getDebugEnv(@NotNull ServerPlayer caster, @NotNull UUID sessionId) {
        return null;
    }

    @Contract(pure = true)
    @Nullable
    default DebugEnvironment getDebugEnv(@NotNull ServerPlayer caster, int threadId) {
        return null;
    }

    /**
     * @throws IllegalDebugSessionException if {@code debugEnv} is currently associated with an
     *     active debug session
     * @throws IllegalDebugThreadException if {@code threadId} is out of range for the player
     *     associated with {@code debugEnv}, or currently associated with an active debug session
     */
    default void createDebugThread(@NotNull DebugEnvironment debugEnv, @Nullable Integer threadId)
        throws IllegalDebugSessionException, IllegalDebugThreadException
    {
        throw new IllegalDebugThreadException();
    }

    /**
     * @throws IllegalDebugSessionException if no debug thread is currently associated with
     *     {@code debugEnv}, or if the debugger is already executing something
     */
    default void startDebuggingIotas(
        @NotNull DebugEnvironment debugEnv,
        @NotNull CastingEnvironment env,
        @NotNull List<Iota> iotas,
        @Nullable CastingImage image
    ) throws IllegalDebugSessionException {
        throw new IllegalDebugSessionException();
    }

    /**
     * Removes a debug thread <strong>without</strong> terminating it.
     */
    default void removeDebugThread(@NotNull DebugEnvironment debugEnv) {}

    /**
     * Terminates and removes a debug thread.
     */
    default void terminateDebugThread(@NotNull DebugEnvironment debugEnv) {}

    default void printDebugMessage(
        @NotNull ServerPlayer caster,
        @NotNull UUID sessionId,
        @NotNull Component message,
        @NotNull DebugOutputCategory category,
        boolean withSource
    ) {}

    // implemented methods

    @Contract(pure = true)
    default boolean isSessionDebugging(@NotNull DebugEnvironment debugEnv) {
        return getDebugEnv(debugEnv.getCaster(), debugEnv.getSessionId()) != null;
    }

    // singleton service loading

    HexDebugCoreAPI INSTANCE = findInstance();

    private static HexDebugCoreAPI findInstance() {
        var providers = ServiceLoader.load(HexDebugCoreAPI.class).stream().toList();
        if (providers.size() > 1) {
            // this should be impossible, barring shenanigans by other addons
            var names = providers.stream()
                .map(p -> p.type().getName())
                .collect(Collectors.joining(",", "[", "]"));
            throw new IllegalStateException(
                "Expected at most one HexDebugCoreAPI implementation on the classpath. Found: " + names
            );
        } else if (providers.size() == 1) {
            // use HexDebug's full API implementation
            return providers.get(0).get();
        } else {
            // fall back to stub implementation if HexDebug isn't present
            return new HexDebugCoreAPI() {};
        }
    }
}
