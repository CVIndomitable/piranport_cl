package com.piranport.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Fuel tank data stored on ship cores.
 * currentFuel and maxFuel are in "b" (bucket) units.
 * One lava bucket = 1b.
 */
public record FuelData(int currentFuel, int maxFuel) {

    public static final Codec<FuelData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("current_fuel").forGetter(FuelData::currentFuel),
            Codec.INT.fieldOf("max_fuel").forGetter(FuelData::maxFuel)
    ).apply(inst, FuelData::new));

    // STREAM_CODEC removed for 1.20.1 (阶段7 NBT重写时处理)

    public FuelData withCurrentFuel(int fuel) {
        return new FuelData(Math.max(0, Math.min(fuel, maxFuel)), maxFuel);
    }

    /** Returns fuel fraction [0, 1]: 0 = empty, 1 = full. */
    public float getFraction() {
        return maxFuel > 0 ? (float) currentFuel / maxFuel : 0f;
    }

    public boolean isEmpty() {
        return currentFuel <= 0;
    }

    public boolean isFull() {
        return currentFuel >= maxFuel;
    }
}
