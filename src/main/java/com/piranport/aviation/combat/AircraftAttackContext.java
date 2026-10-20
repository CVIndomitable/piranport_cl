package com.piranport.aviation.combat;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.ResolvedAircraftStats;
import com.piranport.component.AircraftAttackMode;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 攻击策略能看到的飞机运行时视图。
 *
 * <p>策略只依赖本接口，不直接引用 {@code AircraftEntity} 的包级字段。实体实现本接口，
 * 因此“战术算法”与“实体生命周期”可以分开测试；新增攻击行为时只需实现
 * {@link AttackStrategy} 并注册，不用再改实体里的分发 switch。
 */
public interface AircraftAttackContext {
    AircraftDefinition definition();

    ResolvedAircraftStats stats();

    Level level();

    /** 服务端世界；客户端或非服务端返回 null。 */
    @Nullable ServerLevel serverLevel();

    /** 实体自身，仅用于生成射弹时标记来源或做距离查询。 */
    Entity self();

    Vec3 position();

    Vec3 eyePosition();

    AABB boundingBox();

    Vec3 deltaMovement();

    void setDeltaMovement(Vec3 velocity);

    float yRot();

    float xRot();

    int entityId();

    Component displayName();

    float panelDamage();

    float panelSpeed();

    double phaseSpeedCoefficient();

    int ammoCapacity();

    int remainingAmmo();

    void setRemainingAmmo(int ammo);

    /** 本次挂载是否为核航弹（副本/08）；非水平轰炸机恒为 false。 */
    boolean nuclearPayload();

    int attackCooldown();

    void setAttackCooldown(int ticks);

    int attackCooldownDuration();

    int stateTicks();

    /** 本轮对海火箭航线已用 tick；对空或丢失地面目标时清零。 */
    int rocketRunTicks();

    void setRocketRunTicks(int ticks);

    /** 攻击结束/丢失目标后回到巡航。 */
    void resumeCruising();

    void startReturning(String reason);

    boolean hasFired();

    void setHasFired(boolean fired);

    int salvoSize(LivingEntity target, float damagePerShot);

    AircraftAttackMode attackMode();

    boolean hasEverHadFireControl();

    boolean autoSeekDone();

    void setAutoSeekDone(boolean done);

    int autoSeekCooldown();

    void setAutoSeekCooldown(int ticks);

    @Nullable LivingEntity autonomousTarget();

    @Nullable Player owner();

    @Nullable UUID ownerUuid();

    double distanceToEntity(Entity entity);

    double distanceToEntitySqr(Entity entity);

    @Nullable Vec3 torpedoAimPoint();

    void setTorpedoAimPoint(@Nullable Vec3 point);

    DiveBombState diveBombState();

    TorpedoRunState torpedoRunState();

    LevelBombState levelBombState();
}
