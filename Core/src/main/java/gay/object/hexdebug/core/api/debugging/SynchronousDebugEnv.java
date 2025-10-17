package gay.object.hexdebug.core.api.debugging;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.iota.Iota;
import gay.object.hexdebug.core.api.HexDebugCoreAPI;
import gay.object.hexdebug.core.api.exceptions.DebugException;
import gay.object.hexdebug.core.api.exceptions.IllegalDebugSessionException;
import gay.object.hexdebug.core.api.exceptions.IllegalDebugThreadException;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SynchronousDebugEnv extends DebugEnvironment {
    @NotNull
    private final CastingEnvironment env;
    @NotNull
    private final List<Iota> iotas;

    public SynchronousDebugEnv(
        @NotNull ServerPlayer caster,
        @NotNull CastingEnvironment env,
        @NotNull List<Iota> iotas
    ) {
        super(caster);
        this.env = env;
        this.iotas = iotas;
    }

    @Override
    public boolean pause() {
        return false;
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
    public boolean restart(int threadId) {
        try {
            start(threadId);
            return true;
        } catch (DebugException ignored) {
            return false;
        }
    }

    public void start(@Nullable Integer threadId)
        throws IllegalDebugSessionException, IllegalDebugThreadException
    {
        HexDebugCoreAPI.INSTANCE.createDebugThread(this, threadId);
        HexDebugCoreAPI.INSTANCE.startExecuting(this, env, iotas);
    }
}
