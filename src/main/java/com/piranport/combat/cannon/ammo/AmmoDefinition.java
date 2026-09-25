package com.piranport.combat.cannon.ammo;

import com.piranport.combat.cannon.CannonAmmoRules;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;

/**
 * 炮弹的稳定定义快照。
 *
 * <p>定义以弹药物品的资源 ID 为主键；物品实例、显示名称和运行时配置覆盖不属于定义身份。
 * 这样同一行为的不同口径弹可以共享行为类别，同时保持各自稳定 ID。</p>
 */
public record AmmoDefinition(
        ResourceLocation itemId,
        AmmoBehavior behavior,
        Optional<CannonAmmoRules.CaliberFamily> caliberFamily) {

    public AmmoDefinition {
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(behavior, "behavior");
        Objects.requireNonNull(caliberFamily, "caliberFamily");
        if (itemId.getPath().isEmpty()) {
            throw new IllegalArgumentException("itemId path must not be empty");
        }
    }

    public AmmoDefinition(ResourceLocation itemId, AmmoBehavior behavior) {
        this(itemId, behavior, Optional.empty());
    }

    public AmmoDefinition(ResourceLocation itemId, AmmoBehavior behavior,
                          CannonAmmoRules.CaliberFamily caliberFamily) {
        this(itemId, behavior, Optional.of(Objects.requireNonNull(caliberFamily, "caliberFamily")));
    }

    public boolean isCompatibleWith(CannonAmmoRules.CaliberFamily cannonFamily) {
        Objects.requireNonNull(cannonFamily, "cannonFamily");
        return caliberFamily.isEmpty() || caliberFamily.get() == cannonFamily;
    }

    public boolean isHighExplosive() {
        return behavior == AmmoBehavior.HE || behavior == AmmoBehavior.MK23;
    }

    public boolean isArmorPiercing() {
        return behavior == AmmoBehavior.AP;
    }
}
