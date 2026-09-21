package com.piranport.combat.cannon;

import com.piranport.registry.ModItems;
import net.minecraft.world.item.Item;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.piranport.item.ExperienceShellItem;

/** 火炮数值入口：统一读取世界配置覆盖与经验弹加成。 */
final class CannonStats {
    private CannonStats() {}

    static float getGunDamage(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            float baseDamage = level != null ? ai.getEffectiveData(level).damage() : ai.getDamage();
            return ExperienceShellItem.applyDamageBonus(weapon, baseDamage);
        }
        return 6f;
    }

    static int getGunCooldown(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            com.piranport.artillery.config.ArtilleryCannonData data =
                    level != null ? ai.getEffectiveData(level) : ai.getData();
            return ExperienceShellItem.applyCooldownReduction(
                    weapon, Math.max(data.reloadTime(), data.fireCooldown()));
        }
        return 30;
    }

    static int getBarrelCount(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            com.piranport.artillery.config.ArtilleryCannonData data =
                    level != null ? ai.getEffectiveData(level) : ai.getData();
            return Math.max(data.barrels(), data.salvoCount());
        }
        return 1;
    }

    static int getCannonDurability(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            return level != null ? ai.getEffectiveData(level).durability() : ai.getData().durability();
        }
        return weapon.getMaxDamage();
    }

    static boolean isCannonDamaged(ItemStack weapon, net.minecraft.world.level.Level level) {
        if (!weapon.isDamageableItem()) return false;
        int effectiveDurability = getCannonDurability(weapon, level);
        return weapon.getDamageValue() >= effectiveDurability - 1;
    }

    static boolean isSmallCaliber(ItemStack weapon) {
        return weapon.is(ModItems.JAPANESE_127MM_TWIN_GUN.get()) || weapon.is(ModItems.SINGLE_SMALL_GUN.get());
    }

    static float getExplosionPower(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            float baseExplosion = level != null ? ai.getEffectiveData(level).explosionPower() : ai.getExplosionPower();
            return ExperienceShellItem.applyExplosionBonus(weapon, baseExplosion);
        }
        return 1.0f;
    }

    static float getProjectileVelocity(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            return level != null ? ai.getEffectiveData(level).initialSpeed() : ai.getInitialSpeed();
        }
        return 2.0f;
    }

    static float getProjectileInaccuracy(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            return level != null ? ai.getEffectiveData(level).dispersion() : ai.getDispersionAngle();
        }
        if (isSmallCaliber(weapon)) return 1.5f;
        if (weapon.is(ModItems.MEDIUM_GUN.get())) return 1.0f;
        if (weapon.is(ModItems.LARGE_GUN.get())) return 0.5f;
        return 1.0f;
    }

    static float getVerticalSpread(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            return level != null ? ai.getEffectiveData(level).verticalSpread() : ai.getData().verticalSpread();
        }
        return getProjectileInaccuracy(weapon, level);
    }

    static float getHorizontalSpread(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            return level != null ? ai.getEffectiveData(level).horizontalSpread() : ai.getData().horizontalSpread();
        }
        return getProjectileInaccuracy(weapon, level);
    }

    static double getMinElevationRadians(ItemStack weapon, net.minecraft.world.level.Level level) {
        if (weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem ai) {
            float value = level != null ? ai.getEffectiveData(level).minElevation() : ai.getData().minElevation();
            return Math.toRadians(value);
        }
        return Math.toRadians(-89.0);
    }

    static double getMaxElevationRadians(ItemStack weapon, net.minecraft.world.level.Level level) {
        if (weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem ai) {
            float value = level != null ? ai.getEffectiveData(level).maxElevation() : ai.getData().maxElevation();
            return Math.toRadians(value);
        }
        return Math.toRadians(89.0);
    }

    static float getProjectileGravity(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            return level != null ? ai.getEffectiveData(level).gravity() : ai.getCustomGravity();
        }
        return 0f;
    }

    static float getProjectileDrag(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            return level != null ? ai.getEffectiveData(level).dragCoeff() : ai.getDragCoeff();
        }
        if (isSmallCaliber(weapon)) return 0.015f;
        if (weapon.is(ModItems.MEDIUM_GUN.get())) return 0.01f;
        if (weapon.is(ModItems.LARGE_GUN.get())) return 0.008f;
        return 0.01f;
    }

    static java.util.List<com.piranport.artillery.config.MuzzlePos> getMuzzlePositions(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            java.util.List<com.piranport.artillery.config.MuzzlePos> muzzles =
                    level != null ? ai.getEffectiveData(level).muzzles() : ai.getData().muzzles();
            if (!muzzles.isEmpty()) {
                return muzzles;
            }
        }
        // 默认单炮口位置（向前1.5格）
        return java.util.List.of(new com.piranport.artillery.config.MuzzlePos(0, 0, 1.5));
    }
}
