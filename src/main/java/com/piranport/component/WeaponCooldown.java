package com.piranport.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Stores cooldown state directly on a weapon item (no-GUI mode).
 * Rendered as a durability-style bar by WeaponReloadDecorator.
 */
public record WeaponCooldown(long endTick, int totalTick) {

    public static final WeaponCooldown EMPTY = new WeaponCooldown(0, 1);

    public static final Codec<WeaponCooldown> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.LONG.fieldOf("end_tick").forGetter(WeaponCooldown::endTick),
            Codec.INT.fieldOf("total_tick").forGetter(WeaponCooldown::totalTick)
    ).apply(inst, WeaponCooldown::new));

    public static final StreamCodec<ByteBuf, WeaponCooldown> STREAM_CODEC = StreamCodec.of(
            (buf, wc) -> {
                buf.writeLong(wc.endTick);
                ByteBufCodecs.VAR_INT.encode(buf, wc.totalTick);
            },
            buf -> new WeaponCooldown(buf.readLong(), ByteBufCodecs.VAR_INT.decode(buf))
    );

    public boolean isOnCooldown(long currentTick) {
        return endTick > currentTick;
    }

    /**
     * Returns cooldown fraction [0, 1]: 0 = ready, 1 = just fired.
     */
    public float getFraction(long currentTick) {
        if (endTick <= currentTick) return 0f;
        return Math.min(1f, (float) (endTick - currentTick) / totalTick);
    }
}
