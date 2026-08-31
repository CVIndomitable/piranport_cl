package com.piranport.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Tracks manually-loaded ammo stored on a weapon item (cannon or torpedo launcher).
 * Used in manual reload mode (autoResupplyEnabled = false).
 * count: number of rounds/torpedoes loaded; ammoItemId: registry key of the ammo item.
 */
public record LoadedAmmo(int count, String ammoItemId) {

    private static final int MAX_COUNT = 4096;
    private static final int MAX_ITEM_ID_LENGTH = 256;

    public LoadedAmmo {
        count = Math.max(0, Math.min(count, MAX_COUNT));
        if (ammoItemId == null) ammoItemId = "";
        else if (ammoItemId.length() > MAX_ITEM_ID_LENGTH) throw new IllegalArgumentException("ammoItemId too long");
    }

    public static final LoadedAmmo EMPTY = new LoadedAmmo(0, "");

    public boolean hasAmmo() {
        return count > 0 && !ammoItemId.isEmpty();
    }

    public static final Codec<LoadedAmmo> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("count").forGetter(LoadedAmmo::count),
            Codec.STRING.fieldOf("ammo_item_id").forGetter(LoadedAmmo::ammoItemId)
    ).apply(inst, LoadedAmmo::new));

    public static final StreamCodec<ByteBuf, LoadedAmmo> STREAM_CODEC = StreamCodec.of(
            (buf, la) -> {
                ByteBufCodecs.VAR_INT.encode(buf, la.count());
                ByteBufCodecs.STRING_UTF8.encode(buf, la.ammoItemId());
            },
            buf -> {
                int count = ByteBufCodecs.VAR_INT.decode(buf);
                String ammoId = ByteBufCodecs.stringUtf8(MAX_ITEM_ID_LENGTH).decode(buf);
                return new LoadedAmmo(count, ammoId);
            }
    );
}
