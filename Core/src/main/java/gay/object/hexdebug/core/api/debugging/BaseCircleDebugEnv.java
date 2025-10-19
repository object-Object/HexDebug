package gay.object.hexdebug.core.api.debugging;

import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.NonExtendable
public abstract class BaseCircleDebugEnv extends DebugEnvironment {
    @Nullable
    private CastingImage newImage;

    protected BaseCircleDebugEnv(@NotNull ServerPlayer caster) {
        super(caster);
    }

    @Nullable
    public CastingImage getNewImage() {
        return newImage;
    }

    public void setNewImage(@Nullable CastingImage newImage) {
        this.newImage = newImage;
    }
}
