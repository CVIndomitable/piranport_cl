package com.piranport.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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

    // STREAM_CODEC removed for 1.20.1 (阶段7 NBT重写时处理)
}
