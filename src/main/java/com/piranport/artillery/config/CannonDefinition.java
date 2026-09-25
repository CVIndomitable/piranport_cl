package com.piranport.artillery.config;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 火炮规范定义。
 *
 * <p>资源 ID 与数据必须一起传递，避免调用方把物品注册名、配置文件名和一份
 * 可能已经过期的 {@link ArtilleryCannonData} 混用。定义本身不包含运行时覆盖；
 * 覆盖仍由 {@code ConfigOverrideManager} 叠加在数据上。</p>
 */
public record CannonDefinition(ResourceLocation id, ArtilleryCannonData data) {

    /** 返回所有可在数据包加载阶段发现的错误。空列表表示定义可以作为规范数据源使用。 */
    public List<String> validationErrors() {
        List<String> errors = new ArrayList<>();
        if (id == null) {
            errors.add("missing resource id");
        }
        if (data == null) {
            errors.add("missing cannon data");
            return List.copyOf(errors);
        }

        if (data.caliber() <= 0) errors.add("caliber must be positive");
        if (data.barrels() <= 0) errors.add("barrels must be positive");
        if (data.damage() < 0 || !Float.isFinite(data.damage())) errors.add("damage must be finite and non-negative");
        if (data.reloadTime() < 0) errors.add("reloadTime must be non-negative");
        if (data.durability() <= 0) errors.add("durability must be positive");
        if (data.fireCooldown() < 0) errors.add("fireCooldown must be non-negative");
        if (!Float.isFinite(data.salvoInterval()) || data.salvoInterval() < 0) {
            errors.add("salvoInterval must be finite and non-negative");
        }
        if (!Float.isFinite(data.initialSpeed()) || data.initialSpeed() <= 0) errors.add("initialSpeed must be positive");
        if (!Float.isFinite(data.dragCoeff()) || data.dragCoeff() <= 0) errors.add("dragCoeff must be positive");
        if (!Float.isFinite(data.gravity()) || data.gravity() < 0) errors.add("gravity must be finite and non-negative");
        if (!Float.isFinite(data.explosionPower()) || data.explosionPower() < 0) errors.add("explosionPower must be finite and non-negative");
        if (!Float.isFinite(data.projectileWeight()) || data.projectileWeight() <= 0) errors.add("projectileWeight must be positive");
        if (!Float.isFinite(data.dispersion()) || data.dispersion() < 0) errors.add("dispersion must be finite and non-negative");
        if (!Float.isFinite(data.verticalSpread()) || data.verticalSpread() < 0) errors.add("verticalSpread must be finite and non-negative");
        if (!Float.isFinite(data.horizontalSpread()) || data.horizontalSpread() < 0) errors.add("horizontalSpread must be finite and non-negative");
        if (!Float.isFinite(data.minElevation()) || !Float.isFinite(data.maxElevation()) || data.minElevation() >= data.maxElevation()) {
            errors.add("minElevation must be lower than maxElevation");
        }
        if (data.loadingMode() == null ||
                !("AUTO".equalsIgnoreCase(data.loadingMode()) || "MANUAL".equalsIgnoreCase(data.loadingMode()))) {
            errors.add("loadingMode must be AUTO or MANUAL");
        }

        if (data.muzzles() == null || data.muzzles().size() != data.barrels()) {
            errors.add("muzzles count must equal barrels");
        } else {
            for (int i = 0; i < data.muzzles().size(); i++) {
                MuzzlePos muzzle = data.muzzles().get(i);
                if (muzzle == null || !Double.isFinite(muzzle.x()) || !Double.isFinite(muzzle.y()) || !Double.isFinite(muzzle.z())) {
                    errors.add("muzzles[" + i + "] coordinates must be finite");
                }
            }
        }
        return List.copyOf(errors);
    }

    public boolean isValid() {
        return validationErrors().isEmpty();
    }
}
