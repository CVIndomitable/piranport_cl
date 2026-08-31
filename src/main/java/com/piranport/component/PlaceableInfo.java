package com.piranport.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PlaceableInfo(String containerType, int servings) {
    private static final int MAX_CONTAINER_TYPE_LENGTH = 128;
    private static final int MAX_SERVINGS = 4096;

    public PlaceableInfo {
        if (containerType == null) containerType = "";
        else if (containerType.length() > MAX_CONTAINER_TYPE_LENGTH) throw new IllegalArgumentException("containerType too long");
        servings = Math.clamp(servings, 1, MAX_SERVINGS);
    }

    public static final Codec<PlaceableInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("container_type").forGetter(PlaceableInfo::containerType),
            Codec.INT.fieldOf("servings").forGetter(PlaceableInfo::servings)
    ).apply(i, PlaceableInfo::new));

    public static final StreamCodec<ByteBuf, PlaceableInfo> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_CONTAINER_TYPE_LENGTH), PlaceableInfo::containerType,
            ByteBufCodecs.VAR_INT, PlaceableInfo::servings,
            PlaceableInfo::new
    );
}
