package gay.object.hexdebug.api.client.splicing;

import at.petrak.hexcasting.api.casting.iota.IotaType;
import gay.object.hexdebug.api.splicing.SplicingTableIotaClientView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A factory for {@link SplicingTableIotaRenderer} instances. */
@FunctionalInterface
public interface SplicingTableIotaRendererProvider {
    /**
     * Creates and returns a new {@link SplicingTableIotaRenderer} for the provided iota.
     * <br>
     * May return null if unable to create a renderer for the given iota; in that case, the default
     * renderer will be used instead.
     * <br>
     * This is called every time the splicing table changes which iotas are currently visible, so
     * don't do anything too laggy in here.
     */
    @Nullable
    SplicingTableIotaRenderer createRenderer(
        @NotNull IotaType<?> type,
        @NotNull SplicingTableIotaClientView iota,
        int x,
        int y
    );
}
