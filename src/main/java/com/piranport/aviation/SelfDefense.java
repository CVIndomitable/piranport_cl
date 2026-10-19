package com.piranport.aviation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 自卫机枪的机型配置。
 *
 * <p>{@code zones} 是一组半空间；目标只要落在任一启用分区内就可以开火。
 * 自卫机枪不会为了攻击而改变本机航线，射击节奏与主武器独立。
 *
 * @param zones        启用的覆盖分区，不能为空
 * @param range        射程（格）
 * @param fireCooldown 两次射击的间隔（tick）
 * @param damage       每发子弹伤害
 */
public record SelfDefense(Set<SelfDefenseZone> zones, double range, int fireCooldown, float damage) {
    public static final Codec<SelfDefense> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SelfDefenseZone.CODEC.listOf().fieldOf("zones")
                    .forGetter(selfDefense -> List.copyOf(selfDefense.zones())),
            Codec.DOUBLE.fieldOf("range").forGetter(SelfDefense::range),
            Codec.intRange(1, 1200).fieldOf("fire_cooldown").forGetter(SelfDefense::fireCooldown),
            Codec.FLOAT.fieldOf("damage").forGetter(SelfDefense::damage)
    ).apply(instance, (zones, range, fireCooldown, damage) ->
            new SelfDefense(Set.copyOf(zones), range, fireCooldown, damage)));

    public SelfDefense {
        Objects.requireNonNull(zones, "zones");
        zones = Set.copyOf(zones);
        if (zones.isEmpty()) {
            throw new IllegalArgumentException("self_defense.zones must not be empty");
        }
        if (!Double.isFinite(range) || range <= 0.0) {
            throw new IllegalArgumentException("self_defense.range must be finite and positive");
        }
        if (fireCooldown < 1) {
            throw new IllegalArgumentException("self_defense.fire_cooldown must be positive");
        }
        if (!Float.isFinite(damage) || damage < 0.0F) {
            throw new IllegalArgumentException("self_defense.damage must be finite and non-negative");
        }
    }

    public boolean covers(SelfDefenseZone zone) {
        return zones.contains(zone);
    }
}
