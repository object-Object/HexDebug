package gay.object.hexdebug.core.api.debugging;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.NotNull;

/**
 * An interface to start debugging {@link Block} subclasses when right-clicked by a debugger.
 */
@FunctionalInterface
public interface DebuggableBlock {
    /**
     * Called server-side by {@code DebuggerItem#useOn} to start a debug session for this block on
     * the given thread.
     */
    @NotNull
    InteractionResult startDebugging(@NotNull UseOnContext context, int threadId);
}
