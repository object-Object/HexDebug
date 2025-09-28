package gay.object.hexdebug.api.splicing;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes;
import gay.object.hexdebug.utils.ExtensionsKt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A client-side representation of an iota in the Splicing Table's main list.
 * <br>
 * NOTE: Consumers should not construct this class themselves, as its fields may change at any time.
 * All constructors are annotated with {@link ApiStatus.Internal} to indicate this.
 * @param tag The raw iota NBT.
 * @param name The formatted name of the iota for use in tooltips.
 * @param hexpatternSource The iota converted to a plain string in {@code .hexpattern} format.
 * @param index The index of this iota in the list.
 * @param depth The number of unclosed Introspection patterns preceding this iota.
 */
public record SplicingTableIotaClientView(
    @NotNull CompoundTag tag,
    @NotNull Component name,
    @NotNull String hexpatternSource,
    int index,
    int depth
) {
    @ApiStatus.Internal
    public SplicingTableIotaClientView {}

    @ApiStatus.Internal
    public SplicingTableIotaClientView(
        @NotNull Iota iota,
        @NotNull CastingEnvironment env,
        int index,
        int depth
    ) {
        this(
            IotaType.serialize(iota),
            ExtensionsKt.displayWithPatternName(iota, env),
            ExtensionsKt.toHexpatternSource(iota, env),
            index,
            depth
        );
    }

    @Nullable
    public Tag getData() {
        return tag.get(HexIotaTypes.KEY_DATA);
    }
}
