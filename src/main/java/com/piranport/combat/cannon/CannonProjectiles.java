package com.piranport.combat.cannon;

import com.piranport.config.ModArtilleryConfig;
import com.piranport.entity.CannonProjectileEntity;
import com.piranport.entity.SanshikiPelletEntity;
import com.piranport.network.ShakeEffectPayload;
import com.piranport.server.ScopingManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.neoforge.network.PacketDistributor;
import com.piranport.combat.cannon.CannonAim.*;
import static com.piranport.combat.cannon.CannonStats.getGunDamage;
import static com.piranport.combat.cannon.CannonStats.getCannonDurability;
import static com.piranport.combat.cannon.CannonStats.isSmallCaliber;
import static com.piranport.combat.cannon.CannonStats.getExplosionPower;
import static com.piranport.combat.cannon.CannonStats.getProjectileVelocity;
import static com.piranport.combat.cannon.CannonStats.getVerticalSpread;
import static com.piranport.combat.cannon.CannonStats.getHorizontalSpread;
import static com.piranport.combat.cannon.CannonStats.getProjectileGravity;
import static com.piranport.combat.cannon.CannonStats.getProjectileDrag;
import static com.piranport.combat.cannon.CannonStats.getMuzzlePositions;
import static com.piranport.combat.cannon.CannonSounds.playCannonFireSound;
import static com.piranport.combat.cannon.CannonAmmoRules.isMK23Shell;
import static com.piranport.combat.cannon.CannonAiming.rotateMuzzleByPlayerView;
import com.piranport.combat.cannon.fire.CannonFireRequest;
import com.piranport.combat.cannon.fire.CannonFireService;
import com.piranport.combat.cannon.fire.CannonProjectileFactory;

/** 火炮发射表现：生成炮弹与霰弹，处理粒子、音效和数量上限。 */
final class CannonProjectiles {
    private CannonProjectiles() {}

    static boolean fireCannonSalvo(Level level, Player player, ItemStack weapon,
            ItemStack shellForRender, int barrelCount, boolean isType3, boolean isVT,
            boolean isHE, CannonAim aim) {
        // Phase 11: 全局炮弹上限检测
        if (isShellLimitReached(level, player)) {
            player.displayClientMessage(
                    Component.translatable("message.piranport.max_projectiles"), true);
            return false;
        }
        // Phase 9: 消耗耐久（创造模式不消耗）
        if (!player.getAbilities().instabuild && weapon.isDamageableItem()) {
            int curDamage = weapon.getDamageValue();
            int effectiveDurability = getCannonDurability(weapon, level);
            if (curDamage < effectiveDurability - 1) {
                weapon.setDamageValue(curDamage + 1);
            }
        }

        // 获取炮口位置数据
        java.util.List<com.piranport.artillery.config.MuzzlePos> muzzles = getMuzzlePositions(weapon, level);
        Vec3 referenceMuzzleOffset = rotateMuzzleByPlayerView(player, muzzles.get((muzzles.size() - 1) / 2));
        Vec3 aimOrigin = player.getEyePosition().add(referenceMuzzleOffset);

        float verticalSpreadDeg = getVerticalSpread(weapon, level);
        float horizontalSpreadDeg = getHorizontalSpread(weapon, level);
        // 策划决策/武器/13：闭镜时同样使用弹道解算落点，散布增加10%
        if (!ScopingManager.isScoping(player)) {
            horizontalSpreadDeg *= 1.1f;
            verticalSpreadDeg *= 1.1f;
        }
        for (int b = 0; b < barrelCount; b++) {
            // 计算当前炮管的炮口位置
            com.piranport.artillery.config.MuzzlePos muzzle = muzzles.get(b % muzzles.size());
            Vec3 muzzleOffset = rotateMuzzleByPlayerView(player, muzzle);
            Vec3 spawnPos = player.getEyePosition().add(muzzleOffset);

            if (isType3) {
                boolean spawned = fireSanshikiSpread(level, player, weapon, shellForRender, spawnPos);
                if (!spawned && b == 0) {
                    return false;
                }
            } else {
                float damage = getGunDamage(weapon, level);
                float explosionPower = getExplosionPower(weapon, level);
                // 副本/08 决策：MK23 核炮弹威力 = HE 表值 ×10（写死查表，不走运行时系数）。
                // 仅作用于 LARGE_SHELLS 火炮；isMK23Shell 已在 large_shells 标签上保证。
                if (isMK23Shell(shellForRender)) {
                    explosionPower = explosionPower * 10f;
                }
                float velocity = getProjectileVelocity(weapon, level);
                float drag = getProjectileDrag(weapon, level);
                float gravity = getProjectileGravity(weapon, level);

                CannonFireRequest request = CannonFireService.request(
                        level, player, weapon, shellForRender, damage, explosionPower,
                        velocity, drag, gravity, horizontalSpreadDeg, verticalSpreadDeg,
                        weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem ai
                                ? ai.getEffectiveData(level).caliber() : 0,
                        isHE, isVT, aim, spawnPos);
                if (!CannonFireService.isValid(request)) return false;
                CannonProjectileEntity projectile = CannonProjectileFactory.create(request);
                // 依据：策划决策/数值/05-船型职能分化修订.md（AP 大口径对小型船过穿）
                if (weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem artilleryItem) {
                    projectile.setSourceCaliber(artilleryItem.getEffectiveData(level).caliber());
                }

                Vec3 direction = CannonAiming.resolveDirection(player, weapon, velocity,
                        aim, aimOrigin, spawnPos);
                // 高斯散布：按配置的角度标准差偏转初速度方向
                Vec3 velocityVec = direction.scale(velocity);
                velocityVec = com.piranport.artillery.ArtilleryItem.applyDispersion(
                        velocityVec, level.random, horizontalSpreadDeg, verticalSpreadDeg);

                // 设置炮弹生成位置和速度
                projectile.setPos(spawnPos);
                projectile.shoot(velocityVec.x, velocityVec.y, velocityVec.z, (float) velocityVec.length(), 0f);
                level.addFreshEntity(projectile);
            }

            // 炮口火焰粒子（服务端广播）— 使用实际炮口位置
            if (level instanceof ServerLevel serverLevel) {
                Vec3 look = player.getLookAngle();
                double px = spawnPos.x + look.x * 0.3;
                double py = spawnPos.y + look.y * 0.3;
                double pz = spawnPos.z + look.z * 0.3;
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 3,
                        0.2, 0.2, 0.2, 0.05);
                serverLevel.sendParticles(ParticleTypes.CLOUD, px, py, pz, 2,
                        0.3, 0.15, 0.3, 0.01);
                serverLevel.sendParticles(ParticleTypes.LAVA, px, py, pz, 1,
                        0.1, 0.1, 0.1, 0);
            }

            // Phase 10: 只在第一个炮管播放音效 + 震动（避免齐射重复）
            if (b == 0) {
                playCannonFireSound(level, player, weapon);

                // 发射屏幕震动（S2C）
                float shakeIntensity = isSmallCaliber(weapon, level) ? 0.3f : 0.6f;
                if (player instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer,
                            new ShakeEffectPayload(shakeIntensity, 6));
                }
            }
        }
        return true;
    }

    static boolean isShellLimitReached(Level level, Player player) {
        int maxProjectiles = ModArtilleryConfig.ARTILLERY_MAX_PROJECTILES.get();
        if (maxProjectiles <= 0) return false;
        // 仅在玩家附近搜索（模拟距离范围），避免使用全图(-3e7~3e7) AABB 遍历
        int count = level.getEntitiesOfClass(
                CannonProjectileEntity.class,
                new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(256)).size();
        return count >= maxProjectiles;
    }

    static int getShrapnelLimit() {
        return ModArtilleryConfig.PERF_SHRAPNEL_LIMIT.get();
    }

    static boolean fireSanshikiSpread(Level level, Player player, ItemStack weapon, ItemStack shellForRender, Vec3 spawnPos) {
        float baseDamage = getGunDamage(weapon, level);
        float pelletDamage = baseDamage * 0.25f;
        float velocity = getProjectileVelocity(weapon, level);

        // 霰弹数量限制：统计玩家附近256格内的现有霰弹数，超过上限则不发射
        int targetCount = 64; // 三式弹固定发射64枚霰弹（设计文档要求）
        int limit = getShrapnelLimit();
        if (limit <= 0) return false;
        int existing = level.getEntitiesOfClass(
                SanshikiPelletEntity.class,
                new AABB(player.blockPosition()).inflate(256))
                .size();
        int canSpawn = limit - existing;
        if (canSpawn < targetCount) {
            // 霰弹数量不足，拒绝发射
            player.displayClientMessage(
                    Component.translatable("message.piranport.shrapnel_limit"), true);
            return false;
        }
        int pelletCount = targetCount;

        float baseYaw = player.getYRot();
        float basePitch = player.getXRot();
        float maxRadius = 7.0f; // degrees, same spread range as before
        float goldenAngle = 2.39996323f; // ~137.508 degrees in radians

        for (int i = 0; i < pelletCount; i++) {
            float r = maxRadius * (float) Math.sqrt((i + 0.5f) / pelletCount);
            float theta = i * goldenAngle;
            float yawOffset = r * (float) Math.cos(theta);
            float pitchOffset = r * (float) Math.sin(theta);

            SanshikiPelletEntity pellet = new SanshikiPelletEntity(
                    level, player, pelletDamage, shellForRender);
            pellet.setPos(spawnPos);
            pellet.shootFromRotation(player,
                    basePitch + pitchOffset,
                    baseYaw + yawOffset,
                    0.0f, velocity, 0.5f);
            level.addFreshEntity(pellet);
        }
        return true;
    }
}
