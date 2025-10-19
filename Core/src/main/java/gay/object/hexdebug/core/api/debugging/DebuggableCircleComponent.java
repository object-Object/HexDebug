package gay.object.hexdebug.core.api.debugging;

import at.petrak.hexcasting.api.casting.circles.ICircleComponent;
import at.petrak.hexcasting.api.casting.eval.env.CircleCastEnv;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

public interface DebuggableCircleComponent extends ICircleComponent {
    void acceptDebugControlFlow(
        ServerPlayer caster,
        BaseCircleDebugEnv debugEnv,
        CastingImage imageIn,
        CircleCastEnv env,
        Direction enterDir,
        BlockPos pos,
        BlockState bs
    );
}
