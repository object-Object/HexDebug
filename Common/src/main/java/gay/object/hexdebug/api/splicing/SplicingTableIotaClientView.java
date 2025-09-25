package gay.object.hexdebug.api.splicing;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes;
import gay.object.hexdebug.utils.ExtensionsKt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record SplicingTableIotaClientView(
    @NotNull CompoundTag tag,
    @NotNull Component name,
    @NotNull String hexpatternSource
) {
    public SplicingTableIotaClientView(@NotNull Iota iota, @NotNull CastingEnvironment env) {
        this(
            IotaType.serialize(iota),
            ExtensionsKt.displayWithPatternName(iota, env),
            ExtensionsKt.toHexpatternSource(iota, env)
        );
    }

    @Nullable
    public Tag getData() {
        return tag.get(HexIotaTypes.KEY_DATA);
    }
}
