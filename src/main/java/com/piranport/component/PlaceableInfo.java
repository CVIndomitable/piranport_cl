package com.piranport.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record PlaceableInfo(String containerType, int servings) {
    public static final Codec<PlaceableInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("container_type").forGetter(PlaceableInfo::containerType),
            Codec.INT.fieldOf("servings").forGetter(PlaceableInfo::servings)
    ).apply(i, PlaceableInfo::new));

    public static final StreamCodec<ByteBuf, PlaceableInfo> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PlaceableInfo::containerType,
            ByteBufCodecs.VAR_INT, PlaceableInfo::servings,
            PlaceableInfo::new
    );
}
