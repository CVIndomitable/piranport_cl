package com.piranport.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PlaceableInfo(String containerType, int servings) {
    public static final Codec<PlaceableInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("container_type").forGetter(PlaceableInfo::containerType),
            Codec.INT.fieldOf("servings").forGetter(PlaceableInfo::servings)
    ).apply(i, PlaceableInfo::new));

    // STREAM_CODEC removed for 1.20.1 (阶段7 NBT重写时处理)
}
