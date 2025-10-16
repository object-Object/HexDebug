package gay.object.hexdebug.core.api;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import gay.object.hexdebug.core.api.debugging.DebugEnvironment;
import gay.object.hexdebug.core.api.debugging.DebugOutputCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

class HexDebugCoreAPIStubImpl implements HexDebugCoreAPI {
    @Override
    public @Nullable DebugEnvironment getDebugEnv(@NotNull CastingEnvironment env) {
        return null;
    }

    @Override
    public void printDebugMessage(
        @NotNull ServerPlayer caster,
        @NotNull UUID sessionId,
        @NotNull Component message,
        @NotNull DebugOutputCategory category,
        boolean withSource
    ) {}
}
