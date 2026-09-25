package com.piranport.combat.cannon.fire;

import com.piranport.combat.cannon.CannonAim;
import com.piranport.combat.cannon.CannonAmmoRules;
import com.piranport.entity.CannonProjectileEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 共享火炮发射边界；当前只提供纯校验和实体构造，不改变既有发射事务。 */
public final class CannonFireService {
    private CannonFireService() {}

    /** 返回请求可执行的原因；空字符串表示通过。 */
    public static String validationError(CannonFireRequest request) {
        if (request == null) return "request is null";
        if (request.level().isClientSide()) return "fire request must execute on the server";
        if (!request.shooter().isAlive()) return "shooter is not alive";
        if (request.shooter().level() != request.level()) return "shooter and request level differ";
        if (request.sourceCaliber() == 0) return "source caliber is unspecified";
        return "";
    }

    public static boolean isValid(CannonFireRequest request) {
        return validationError(request).isEmpty();
    }

    /** 供数据解析和调用方在创建世界请求前复用的物理参数校验。 */
    public static String validatePhysics(float damage, float explosionPower, float velocity,
            float drag, float gravity, float horizontalSpread, float verticalSpread, int sourceCaliber) {
        if (!Float.isFinite(damage) || damage < 0) return "damage must be finite and non-negative";
        if (!Float.isFinite(explosionPower) || explosionPower < 0) return "explosionPower must be finite and non-negative";
        if (!Float.isFinite(velocity) || velocity <= 0) return "velocity must be positive";
        if (!Float.isFinite(drag) || drag <= 0) return "drag must be positive";
        if (!Float.isFinite(gravity) || gravity < 0) return "gravity must be finite and non-negative";
        if (!Float.isFinite(horizontalSpread) || horizontalSpread < 0 ||
                !Float.isFinite(verticalSpread) || verticalSpread < 0) return "spread must be finite and non-negative";
        if (sourceCaliber < 0) return "sourceCaliber must be non-negative";
        return "";
    }

    /** 使用请求快照创建实体；速度方向和散布由调用方在生成前计算。 */
    public static CannonProjectileEntity createProjectile(CannonFireRequest request) {
        String error = validationError(request);
        if (!error.isEmpty()) throw new IllegalArgumentException(error);
        CannonProjectileEntity projectile = new CannonProjectileEntity(
                request.level(), request.shooter(), request.shell(), request.damage(),
                request.highExplosive(), request.explosionPower());
        projectile.setVT(request.proximityFuse());
        projectile.setDragCoeff(request.drag());
        projectile.setCustomGravity(request.gravity());
        projectile.setSourceCaliber(request.sourceCaliber());
        projectile.setPos(request.spawnPosition());
        return projectile;
    }

    /** 便于未来玩家、女仆调用方从同一组数值创建请求。 */
    public static CannonFireRequest request(Level level, LivingEntity shooter, ItemStack weapon,
            ItemStack shell, float damage, float explosionPower, float velocity, float drag,
            float gravity, float horizontalSpread, float verticalSpread, int sourceCaliber,
            boolean highExplosive, boolean proximityFuse, CannonAim aim, Vec3 spawnPosition) {
        return new CannonFireRequest(level, shooter, weapon, shell, damage, explosionPower, velocity,
                drag, gravity, horizontalSpread, verticalSpread, sourceCaliber, highExplosive,
                proximityFuse, aim, spawnPosition);
    }
}
