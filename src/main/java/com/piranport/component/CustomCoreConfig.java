package com.piranport.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.piranport.item.ShipType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 自定义核心配置数据组件，用于存储改装后的槽位配置。
 * 存储在舰装核心物品上，记录玩家通过改装器定制的槽位数量和舰型。
 */
public record CustomCoreConfig(
        ShipType baseType,           // 基础舰型
        int customWeaponSlots,       // 自定义武器槽数量（2-8）
        int customEnhancementSlots,  // 自定义强化槽数量（1-6）
        boolean isCustomized         // 是否已改装
) {
    public CustomCoreConfig {
        baseType = baseType == null ? ShipType.SMALL : baseType;
        customWeaponSlots = Math.clamp(customWeaponSlots, MIN_WEAPON_SLOTS, MAX_WEAPON_SLOTS);
        customEnhancementSlots = Math.clamp(customEnhancementSlots, MIN_ENHANCEMENT_SLOTS, MAX_ENHANCEMENT_SLOTS);
        while (customWeaponSlots + baseType.ammoSlots + customEnhancementSlots > MAX_TOTAL_SLOTS) {
            if (customWeaponSlots > MIN_WEAPON_SLOTS) {
                --customWeaponSlots;
            } else {
                --customEnhancementSlots;
            }
        }
    }

    // ===== 槽位限制常量 =====
    public static final int MIN_WEAPON_SLOTS = 2;
    public static final int MAX_WEAPON_SLOTS = 8;
    public static final int MIN_ENHANCEMENT_SLOTS = 1;
    public static final int MAX_ENHANCEMENT_SLOTS = 6;
    public static final int MAX_TOTAL_SLOTS = 18; // 武器+弹药+强化

    // ===== Codec 序列化 =====
    public static final Codec<CustomCoreConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.xmap(
                            name -> { try { return ShipType.valueOf(name); } catch (IllegalArgumentException e) { return ShipType.SMALL; } },
                            ShipType::name
                    ).fieldOf("baseType").forGetter(CustomCoreConfig::baseType),
                    Codec.INT.fieldOf("customWeaponSlots").forGetter(CustomCoreConfig::customWeaponSlots),
                    Codec.INT.fieldOf("customEnhancementSlots").forGetter(CustomCoreConfig::customEnhancementSlots),
                    Codec.BOOL.fieldOf("isCustomized").forGetter(CustomCoreConfig::isCustomized)
            ).apply(instance, CustomCoreConfig::new)
    );

    // ===== StreamCodec 网络传输 =====
    public static final StreamCodec<ByteBuf, CustomCoreConfig> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(CustomCoreConfig::shipTypeByOrdinal, Enum::ordinal),
            CustomCoreConfig::baseType,
            ByteBufCodecs.VAR_INT,
            CustomCoreConfig::customWeaponSlots,
            ByteBufCodecs.VAR_INT,
            CustomCoreConfig::customEnhancementSlots,
            ByteBufCodecs.BOOL,
            CustomCoreConfig::isCustomized,
            CustomCoreConfig::new
    );

    private static ShipType shipTypeByOrdinal(int index) {
        ShipType[] values = ShipType.values();
        return index >= 0 && index < values.length ? values[index] : ShipType.SMALL;
    }

    // ===== 工厂方法 =====

    /**
     * 创建默认配置（未改装）
     */
    public static CustomCoreConfig createDefault(ShipType type) {
        return new CustomCoreConfig(type, type.weaponSlots, type.enhancementSlots, false);
    }

    /**
     * 创建自定义配置（已改装）
     */
    public static CustomCoreConfig createCustom(ShipType baseType, int weaponSlots, int enhancementSlots) {
        return new CustomCoreConfig(baseType, weaponSlots, enhancementSlots, true);
    }

    // ===== 验证方法 =====

    /**
     * 验证槽位配置是否合法
     */
    public boolean isValid() {
        if (customWeaponSlots < MIN_WEAPON_SLOTS || customWeaponSlots > MAX_WEAPON_SLOTS) {
            return false;
        }
        if (customEnhancementSlots < MIN_ENHANCEMENT_SLOTS || customEnhancementSlots > MAX_ENHANCEMENT_SLOTS) {
            return false;
        }
        // 检查总槽位数（武器+弹药+强化）
        int totalSlots = customWeaponSlots + baseType.ammoSlots + customEnhancementSlots;
        return totalSlots <= MAX_TOTAL_SLOTS;
    }

    /**
     * 获取总槽位数
     */
    public int totalSlots() {
        return customWeaponSlots + baseType.ammoSlots + customEnhancementSlots;
    }

    /**
     * 获取武器槽位数（如果已改装则返回自定义值，否则返回基础值）
     */
    public int getWeaponSlots() {
        return isCustomized ? customWeaponSlots : baseType.weaponSlots;
    }

    /**
     * 获取强化槽位数（如果已改装则返回自定义值，否则返回基础值）
     */
    public int getEnhancementSlots() {
        return isCustomized ? customEnhancementSlots : baseType.enhancementSlots;
    }

    /**
     * 获取弹药槽位数（始终使用基础舰型的值）
     */
    public int getAmmoSlots() {
        return baseType.ammoSlots;
    }
}
