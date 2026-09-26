package com.piranport.combat.cannon.fire;

import com.piranport.combat.cannon.CannonAim;
import com.piranport.combat.cannon.CannonAmmoRules;
import com.piranport.combat.cannon.ammo.AmmoDefinitionService;
import net.minecraft.core.registries.BuiltInRegistries;
import com.piranport.entity.CannonProjectileEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.piranport.config.ModArtilleryConfig;
import com.piranport.entity.SanshikiPelletEntity;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;

/** 共享火炮发射边界。 */
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

    public record Shot(CannonFireRequest request, Vec3 direction, boolean type3) {}
    public record Result(int shots, int entities, boolean complete) {}

    public static int entityCount(boolean type3) { return type3 ? 64 : 1; }

    public static boolean withinLimit(int existing, int requested, int limit) {
        return requested == 0 || limit > 0 && existing >= 0 && requested <= limit - existing;
    }

    /** 整轮预检；不占用弹药，也不产生实体。调用方应在同一服务端线程上立即提交。 */
    public static String validatePlan(List<Shot> shots) {
        if (shots == null || shots.isEmpty()) return "empty salvo";
        Level level = shots.getFirst().request().level();
        LivingEntity shooter = shots.getFirst().request().shooter();
        ItemStack weapon = shots.getFirst().request().weapon();
        if (!(weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem artillery)
                || shots.size() > Math.max(1, artillery.getEffectiveData(level).barrels())) return "invalid barrel count";
        for (Shot shot : shots) {
            String error = validationError(shot.request());
            if (!error.isEmpty()) return error;
            if (shot.request().level() != level || shot.request().shooter() != shooter) return "mixed salvo owner";
            if (!ItemStack.isSameItemSameComponents(weapon, shot.request().weapon())) return "mixed salvo weapon";
            if (!shot.request().shooter().isAlive()) return "shooter is not alive";
            if (!CannonAmmoRules.matchesCaliber(shot.request().shell(), shot.request().weapon(), level)) return "invalid shell caliber";
            if (!finite(shot.direction()) || shot.direction().lengthSqr() < 1.0E-12) return "invalid direction";
            if (shot.type3() != CannonAmmoRules.isType3Shell(shot.request().shell())) return "shell behavior mismatch";
            if (artillery.getEffectiveData(level).caliber() != shot.request().sourceCaliber()) return "source caliber mismatch";
        }
        return "";
    }

    private static boolean finite(Vec3 vector) {
        return vector != null && Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    /** 返回实际成功插入的炮管数；部分插入不伪装成完整齐射。 */
    public static Result emit(List<Shot> shots) {
        if (!validatePlan(shots).isEmpty()) return new Result(0, 0, false);
        Level level = shots.getFirst().request().level();
        AABB area = new AABB(shots.getFirst().request().shooter().blockPosition()).inflate(256);
        int projectileCount = level.getEntitiesOfClass(CannonProjectileEntity.class, area).size();
        int pelletCount = level.getEntitiesOfClass(SanshikiPelletEntity.class, area).size();
        int projectileLimit = ModArtilleryConfig.ARTILLERY_MAX_PROJECTILES.get();
        int pelletLimit = ModArtilleryConfig.PERF_SHRAPNEL_LIMIT.get();
        var definition = shots.stream().map(shot -> AmmoDefinitionService.find(
                BuiltInRegistries.ITEM.getKey(shot.request().shell().getItem()))).toList();
        int[] shotIndex = {0};
        return transact(shots, shot -> {
            CannonFireRequest request = shot.request();
            var ammo = definition.get(shotIndex[0]++);
            Vec3 direction = shot.direction().normalize().scale(request.velocity());
            if (shot.type3()) {
                float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
                float pitch = (float) Math.toDegrees(-Math.asin(Math.max(-1, Math.min(1, direction.normalize().y))));
                return insertBatch(64, index -> {
                    float radius = 7f * (float) Math.sqrt((index + .5f) / 64);
                    float angle = index * 2.39996323f;
                    float multiplier = ammo.map(d -> d.damageMultiplier()).orElse(1f);
                    SanshikiPelletEntity pellet = CannonProjectileFactory.createSanshikiPellet(request.level(), request.shooter(), request.damage() * multiplier * .25f, request.shell());
                    pellet.setPos(request.spawnPosition());
                    pellet.shootFromRotation(request.shooter(), pitch + radius * (float) Math.sin(angle), yaw + radius * (float) Math.cos(angle), 0, request.velocity(), .5f);
                    return pellet;
                }, request.level()::addFreshEntity, Entity::discard);
            } else {
                var projectile = createProjectile(request, ammo);
                Vec3 dispersed = com.piranport.artillery.ArtilleryItem.applyDispersion(direction, request.level().random, request.horizontalSpread(), request.verticalSpread());
                projectile.shoot(dispersed.x, dispersed.y, dispersed.z, (float) dispersed.length(), 0);
                return request.level().addFreshEntity(projectile);
            }
        }, projectileCount, projectileLimit, pelletCount, pelletLimit);
    }

    static <T> boolean insertBatch(int count, java.util.function.IntFunction<T> create,
            java.util.function.Predicate<T> add, java.util.function.Consumer<T> rollback) {
        List<T> inserted = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            T entity = create.apply(index);
            if (!add.test(entity)) {
                inserted.forEach(rollback);
                return false;
            }
            inserted.add(entity);
        }
        return true;
    }

    /** 每发完整提交；容量不足或插入失败时只保留已完成的前缀。 */
    static <T> Result transact(List<T> shots, Function<T, Boolean> insert,
            int projectiles, int projectileLimit, int pellets, int pelletLimit,
            Function<T, Boolean> isType3) {
        int committed = 0;
        int entities = 0;
        for (T shot : shots) {
            boolean type3 = isType3.apply(shot);
            int count = entityCount(type3);
            if ((type3 || projectileLimit > 0)
                    && !withinLimit(type3 ? pellets : projectiles, count, type3 ? pelletLimit : projectileLimit)) break;
            if (!insert.apply(shot)) break;
            committed++;
            entities += count;
            if (type3) pellets += count;
            else projectiles += count;
        }
        return new Result(committed, entities, committed == shots.size());
    }

    private static Result transact(List<Shot> shots, Function<Shot, Boolean> insert,
            int projectiles, int projectileLimit, int pellets, int pelletLimit) {
        return transact(shots, insert, projectiles, projectileLimit, pellets, pelletLimit, Shot::type3);
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
        return createProjectile(request, AmmoDefinitionService.find(BuiltInRegistries.ITEM.getKey(request.shell().getItem())));
    }

    private static CannonProjectileEntity createProjectile(CannonFireRequest request,
            java.util.Optional<com.piranport.combat.cannon.ammo.AmmoDefinition> definition) {
        float damage = request.damage() * definition.map(d -> d.damageMultiplier()).orElse(1f);
        float explosionPower = request.explosionPower() * definition.map(d -> d.explosionMultiplier()).orElse(1f);
        CannonProjectileEntity projectile = new CannonProjectileEntity(
                request.level(), request.shooter(), request.shell(), damage,
                request.highExplosive(), explosionPower);
        projectile.setDamage(damage);
        projectile.setVT(request.proximityFuse());
        definition.ifPresent(d -> {
            projectile.setArmorIgnore(d.armorIgnore());
            projectile.setUnderwaterExplosion(d.underwaterExplosion());
        });
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
