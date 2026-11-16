package com.piranport.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Stores cooldown state directly on a weapon item.
 * Rendered by the hotbar reload bar (ReloadProgressHudLayer).
 */
public record WeaponCooldown(long endTick, int totalTick) {

    public WeaponCooldown {
        if (endTick < 0) endTick = 0;
        if (totalTick < 1) totalTick = 1;
    }

    public static final WeaponCooldown EMPTY = new WeaponCooldown(0, 1);

    /**
     * Create a cooldown starting from {@code currentTick} lasting {@code ticks}.
     */
    public static WeaponCooldown of(long currentTick, int ticks) {
        return of(null, currentTick, ticks);
    }

    /** Compatibility overload for callers that track the player who triggered the cooldown. */
    public static WeaponCooldown of(java.util.UUID owner, long currentTick, int ticks) {
        return new WeaponCooldown(currentTick + ticks, ticks);
    }

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
    /**
     * Returns reload progress [0, 1]: 0 = just fired, 1 = ready.
     */
    public float getProgress(long currentTick) {
        return 1f - getFraction(currentTick);
    }

    /**
     * Returns remaining cooldown ticks (0 if ready).
     */
    public float getRemainingTicks(long currentTick) {
        return Math.max(0, endTick - currentTick);
    }

    public float getFraction(long currentTick) {
        if (endTick <= currentTick) return 0f;
        return Math.min(1f, (float) (endTick - currentTick) / totalTick);
    }
}
