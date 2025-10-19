package gay.object.hexdebug.core.api.debugging;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.iota.Iota;
import gay.object.hexdebug.core.api.HexDebugCoreAPI;
import gay.object.hexdebug.core.api.exceptions.DebugException;
import gay.object.hexdebug.core.api.exceptions.IllegalDebugSessionException;
import gay.object.hexdebug.core.api.exceptions.IllegalDebugThreadException;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SimplePlayerBasedDebugEnv extends DebugEnvironment {
    @NotNull
    private final CastingEnvironment env;
    @NotNull
    private final List<Iota> iotas;
    @NotNull
    private final Component name;

    public SimplePlayerBasedDebugEnv(
        @NotNull ServerPlayer caster,
        @NotNull CastingEnvironment env,
        @NotNull List<Iota> iotas,
        @NotNull Component name
        ) {
        super(caster);
        this.env = env;
        this.iotas = iotas;
        this.name = name;
    }

    @Override
    public boolean resume(
        @NotNull CastingEnvironment env,
        @NotNull CastingImage image,
        @NotNull ResolvedPatternType resolutionType
    ) {
        return false;
    }

    @Override
    public void restart(int threadId) {
        try {
            start(threadId);
        } catch (DebugException ignored) {}
    }

    @Override
    public void terminate() {}

    @Override
    public boolean isCasterInRange() {
        return true;
    }

    @Override
    @NotNull
    public Component getName() {
        return name;
    }

    public void start(@Nullable Integer threadId)
        throws IllegalDebugSessionException, IllegalDebugThreadException
    {
        HexDebugCoreAPI.INSTANCE.createDebugThread(this, threadId);
        HexDebugCoreAPI.INSTANCE.startDebuggingIotas(this, env, iotas, null);
    }
}
