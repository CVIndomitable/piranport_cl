package com.piranport.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 海上 R 键鱼雷装填的「待结算」状态（策划决策/武器/鱼雷-再装填机制，2026-10-19 修订）。
 *
 * <p>读条期间不扣背包物品，读完才扣；读条不可取消。因此开读条时只在本组件记下弹种和数量，
 * 由 {@code WeaponReloadLifecycle} 在 WEAPON_COOLDOWN 到期后统一扣除并写入膛内弹药。</p>
 */
public record PendingTorpedoReload(int count, String ammoItemId) {
    private static final int MAX_COUNT = 4096;
    private static final int MAX_ITEM_ID_LENGTH = 256;

    public PendingTorpedoReload {
        count = Math.max(0, Math.min(count, MAX_COUNT));
        if (ammoItemId == null) ammoItemId = "";
        else if (ammoItemId.length() > MAX_ITEM_ID_LENGTH) {
            throw new IllegalArgumentException("ammoItemId too long");
        }
    }

    public boolean isValid() {
        return count > 0 && !ammoItemId.isEmpty();
    }

    public static final Codec<PendingTorpedoReload> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("count").forGetter(PendingTorpedoReload::count),
            Codec.STRING.fieldOf("ammo_item_id").forGetter(PendingTorpedoReload::ammoItemId)
    ).apply(inst, PendingTorpedoReload::new));

    public static final StreamCodec<ByteBuf, PendingTorpedoReload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.VAR_INT.encode(buf, p.count());
                ByteBufCodecs.STRING_UTF8.encode(buf, p.ammoItemId());
            },
            buf -> new PendingTorpedoReload(ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.stringUtf8(MAX_ITEM_ID_LENGTH).decode(buf))
    );
}
