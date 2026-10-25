package gay.object.hexdebug.core.api.debugging;

import at.petrak.hexcasting.api.casting.eval.env.CircleCastEnv;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.common.blocks.circles.BlockEntitySlate;
import gay.object.hexdebug.core.api.debugging.env.BaseCircleDebugEnv;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@FunctionalInterface
public interface SlateDebugIotasResolver {
    /**
     * Called in {@code BlockSlate.acceptDebugControlFlow} to get the list of iotas to execute for
     * this slate, or null if this resolver does not want to provide iotas for this slate.
     * If multiple resolvers return a value for the same slate, the first one registered wins.
     */
    @Nullable
    List<@NotNull Iota> getIotas(
        @NotNull ServerPlayer caster,
        @NotNull BaseCircleDebugEnv debugEnv,
        @NotNull CastingImage imageIn,
        @NotNull CircleCastEnv env,
        @NotNull Direction enterDir,
        @NotNull BlockPos pos,
        @NotNull BlockState bs,
        @NotNull BlockEntitySlate tile
    );
}
