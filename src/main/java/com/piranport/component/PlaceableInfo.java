package com.piranport.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 可放置食物数据组件。依据：策划决策/食物/09-食物方块饱食度加成.md
 *
 * <p>字段：</p>
 * <ul>
 *   <li>containerType: 容器类型（plate/bowl/cake）</li>
 *   <li>servings: 可使用次数（每吃一口减 1）</li>
 *   <li>bonusMultiplier: 方块版本饱食度相对手持的加成系数（默认 1.5）</li>
 * </ul>
 *
 * <p>策划可在物品数据 component 中覆盖 bonusMultiplier（如菜品 JSON 中指定）；默认不指定时按占位规则沿用 1.5x。</p>
 */
public record PlaceableInfo(String containerType, int servings, float bonusMultiplier) {
    private static final int MAX_CONTAINER_TYPE_LENGTH = 128;
    private static final int MAX_SERVINGS = 4096;
    /** 默认方块加成（手持 1.0 → 方块 1.5x） */
    public static final float DEFAULT_BONUS = 1.5f;
    private static final float MIN_BONUS = 1.0f;
    private static final float MAX_BONUS = 10.0f;

    /** 兼容旧数据（缺省 bonusMultiplier 时取 1.5x） */
    public PlaceableInfo(String containerType, int servings) {
        this(containerType, servings, DEFAULT_BONUS);
    }

    public PlaceableInfo {
        if (containerType == null) containerType = "";
        else if (containerType.length() > MAX_CONTAINER_TYPE_LENGTH) {
            throw new IllegalArgumentException("containerType too long");
        }
        servings = Math.clamp(servings, 1, MAX_SERVINGS);
        if (!Float.isFinite(bonusMultiplier) || bonusMultiplier < MIN_BONUS) bonusMultiplier = DEFAULT_BONUS;
        else if (bonusMultiplier > MAX_BONUS) bonusMultiplier = MAX_BONUS;
    }

    public static final Codec<PlaceableInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("container_type").forGetter(PlaceableInfo::containerType),
            Codec.INT.fieldOf("servings").forGetter(PlaceableInfo::servings),
            Codec.FLOAT.optionalFieldOf("bonus_multiplier", DEFAULT_BONUS).forGetter(PlaceableInfo::bonusMultiplier)
    ).apply(i, PlaceableInfo::new));

    public static final StreamCodec<ByteBuf, PlaceableInfo> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_CONTAINER_TYPE_LENGTH), PlaceableInfo::containerType,
            ByteBufCodecs.VAR_INT, PlaceableInfo::servings,
            ByteBufCodecs.FLOAT, PlaceableInfo::bonusMultiplier,
            PlaceableInfo::new
    );
}