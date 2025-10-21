package gay.object.hexdebug.core.api.debugging;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

/**
 * An interface for a {@link Block} or {@link BlockEntity} that can start debugging when
 * right-clicked by a debugger.
 */
public interface DebuggableBlock {
    /**
     * Called server-side by {@code DebuggerItem#useOn} to start a debug session for this block on
     * the given thread.
     * <br>
     * By default, this just calls {@link DebuggableBlock#startDebugging(ServerPlayer, int)}.
     */
    @NotNull
    default InteractionResult startDebugging(@NotNull UseOnContext context, int threadId) {
        return startDebugging((ServerPlayer) context.getPlayer(), threadId);
    }

    /**
     * Called server-side by the default implementation of
     * {@link DebuggableBlock#startDebugging(UseOnContext, int)} to start a debug session for this
     * block on the given thread.
     */
    @NotNull
    InteractionResult startDebugging(@NotNull ServerPlayer caster, int threadId);
}
