package gay.object.hexdebug.core.api.debugging;

import at.petrak.hexcasting.api.casting.eval.env.CircleCastEnv;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import gay.object.hexdebug.core.api.debugging.env.BaseCircleDebugEnv;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public interface DebuggableCircleComponent {
    void acceptDebugControlFlow(
        @NotNull ServerPlayer caster,
        @NotNull BaseCircleDebugEnv debugEnv,
        @NotNull CastingImage imageIn,
        @NotNull CircleCastEnv env,
        @NotNull Direction enterDir,
        @NotNull BlockPos pos,
        @NotNull BlockState bs
    );
}
