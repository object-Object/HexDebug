package gay.object.hexdebug.core.api.debugging;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class SynchronousDebugEnv extends DebugEnvironment {
    public SynchronousDebugEnv(@NotNull ServerPlayer caster) {
        super(caster);
    }

    @Override
    public boolean continueCast(@NotNull CastingEnvironment env, @NotNull CastingImage image) {
        return false;
    }
}
