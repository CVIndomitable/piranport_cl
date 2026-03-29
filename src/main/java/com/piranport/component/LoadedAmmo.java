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
            buf -> new LoadedAmmo(
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf)
            )
    );
}
