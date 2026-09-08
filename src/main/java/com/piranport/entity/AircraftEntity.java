package com.piranport.entity;

import com.piranport.PiranPort;
import com.piranport.aviation.FireControlManager;
import net.minecraft.core.registries.BuiltInRegistries;
import com.piranport.aviation.ReconManager;
import com.piranport.component.AircraftAttackMode;
import com.piranport.component.AircraftInfo;
import com.piranport.item.ExperienceShellItem;
import com.piranport.network.AswSonarSyncPayload;
import com.piranport.network.ReconStatePayload;
import com.piranport.platform.ClientHooks;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 飞机实体 — 服务端权威控制，客户端插值渲染。
 *
 * 架构导航（维护者阅读入口）：
 *   - 状态机:   tickLaunching → tickCruising → tickAttacking → tickReturning
 *   - 战斗分发: tickAttacking 根据 payloadType 委托各攻击策略
 *       战斗机对空扫射 / 火箭机齐射 / 俯冲轰炸 / 水平轰炸 / 鱼雷投掷 / 反潜深弹 / 侦察
 *   - 火控:     resolveTarget / resolveASWTarget / resolveFighterTarget
 *   - 侦察:     tickReconActive + ReconManager（服务端），ClientReconData（客户端）
 *   - 区块加载: updateReconChunkLoading / sendPendingChunks（基于视距）
 *   - 防御:     stuck 检测 / 距离限制 / 最大留空时间
 *   - 持久化:   addAdditionalSaveData / readAdditionalSaveData（约 100 个字段）
 *   - 客户端:   lerpTo 重写 + tick() 客户端分支（防镜头抖动）
 *   - ASW 声呐: 水下目标搜索与标记
 *
 * 关键交互面:
 *   - ReconManager（服务端侦察状态）
 *   - FireControlManager（火控锁定）
 *   - AircraftIndex（按 owner UUID 查找飞机）
 *   - AircraftAttackMode（FOCUS / SPREAD / FOLLOW）
 *   - AircraftInfo（从物品 NBT 读取面板伤害/速度/弹药）
 */
public class AircraftEntity extends Entity {

    public enum FlightState {
        LAUNCHING, CRUISING, ATTACKING, RETURNING, REMOVED,
        RECON_ACTIVE  // Phase 32 — ordinal 5, must remain last
    }

    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AIRCRAFT_TYPE_DATA =
            SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_ID =
            SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> WEAPON_SLOT_INDEX =
            SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.INT);

    // Saved to NBT
    @Nullable private UUID ownerUUID;
    private int weaponSlotIndex = -1;  // -1表示未设置，避免误用默认值0
    private int coreInventorySlot = 0;
    AircraftInfo.AircraftType aircraftType = AircraftInfo.AircraftType.FIGHTER;
    AircraftAttackMode attackMode = AircraftAttackMode.FOCUS;
    float panelDamage;
    float panelSpeed;
    int remainingAmmo = 0;
    int ammoCapacity = 0;
    private int currentFuel = 0;
    private int fuelCapacity = 1;
    boolean hasBullets = false;
    String payloadType = "";
    AircraftInfo.BombingMode bombingMode = AircraftInfo.BombingMode.DIVE;

    // 运行时（不保存）
    private int airtimeTicks = 0;
    private int stuckTicks = 0;
    private Vec3 stuckCheckPos = Vec3.ZERO;
    private double orbitAngle = 0;
    int stateTicks = 0;
    int attackCooldown = 0;
    boolean hasFired = false;
    boolean diveCommitted = false;
    @Nullable Vec3 diveTarget = null;

    // 水平轰炸机投弹状态
    boolean levelBombDropped = false;
    @Nullable Vec3 levelRunDirection = null;
    @Nullable Vec3 levelDropPoint = null;
    int levelRunTicks = 0;
    int autoSeekCooldown = 20; // 运行时状态，避免首次 tick 立即搜索（不持久化，每次加载重置为 20）
    boolean autoSeekDone = false;    // true after first auto-seek scan (one-shot)
    private boolean appliedSlowness = false; // tracks if slowness effect was applied to target
    boolean hasEverHadFireControl = false; // true if FC target was ever assigned
    private transient net.minecraft.world.item.Item cachedPayloadItem; // P3 #31: cached Item reference

    // Phase 32 runtime fields
    private FlightState lastKnownState = FlightState.LAUNCHING;
    int lastForcedChunkX = Integer.MIN_VALUE;
    int lastForcedChunkZ = Integer.MIN_VALUE;

    // 侦察区块加载：基于视距的强制加载 + 区块发送
    final java.util.Set<Long> reconForcedChunks = new java.util.HashSet<>();
    final java.util.Set<Long> reconPendingSend = new java.util.HashSet<>();
    private static final int RECON_CHUNK_SEND_RATE = 16; // max chunks to send per tick

    // Phase 33: aircraft health (persisted to NBT)
    int aircraftHealth = 20;

    // P3 #6: cached recon aircraft reference for FOLLOW mode (refreshed every 20 ticks)
    @Nullable private AircraftEntity cachedReconAircraft;
    private int reconCacheTick = 0;

    // 侦察地图槽缓存：上次扫描时拥有者背包中持有 MapItem 的槽位索引
    // Full-inventory scan (41 slots) is deferred to once per RECON_MAP_REFRESH_TICKS.
    @Nullable int[] cachedMapSlots;
    int mapSlotsRefreshTick = -RECON_MAP_REFRESH_TICKS;
    private static final int RECON_MAP_REFRESH_TICKS = 100;

    // 客户端位置插值（防止原始 setPos 跳跃导致的镜头抖动）
    private int clientLerpSteps;
    private double clientLerpX, clientLerpY, clientLerpZ;
    private float clientLerpYRot, clientLerpXRot;

    // P2 #29: preserve original ItemStack across launch-return cycle
    private ItemStack originalStack = ItemStack.EMPTY;

    // Phase 34: set to true when defense mechanisms force a recall (stuck, timeout, distance)
    private boolean isForcedReturn = false;

    // P0-3: 飞机存活时间（毫秒），用于 REMOVED 埋点
    private final long spawnTimeMs = System.currentTimeMillis();

    // P0-3: 物品返还结果（SUCCESS / SLOT_FULL / OWNER_OFFLINE），供 REMOVED 关联
    private String lastReturnResult = null;

    // AircraftIndex 登记：通过 onAddedToLevel 或 tick 回退添加后置为 true
    private boolean indexRegistered = false;

    // 自主模式：飞机无玩家所属（如浮靶或女仆发射的飞机）
    boolean autonomous = false;
    Vec3 homePosition = Vec3.ZERO;
    @Nullable private Vec3 lastHorizontalDir = null; // previous tick's horizontal direction for turn radius
    @Nullable LivingEntity autonomousTarget = null;

    private static final int MAX_AIRTIME_TICKS = 12000;
    private static final double MIN_DIST_FROM_OWNER = 48.0;
    private static final double MIN_RECON_DIST = 200.0;  // Phase 32

    /**
     * 动态距离上限：取 max(最小值, (服务器模拟距离 - 1) × 16)。
     * 留 1 区块余量防止飞机进入无法 tick 的边界。
     */
    private double getDistanceLimit(double minimum) {
        int simDist = 10;
        if (level() instanceof ServerLevel sl && sl.getServer() != null) {
            simDist = sl.getServer().getPlayerList().getSimulationDistance();
        }
        return Math.max(minimum, (simDist - 1) * 16.0);
    }
    private static final int STUCK_CHECK_INTERVAL = 60;
    private static final double STUCK_THRESHOLD = 0.1;
    private static final int LAUNCH_DURATION = 30;
    private static final double CRUISE_ALTITUDE = 18.0;
    private static final double ORBIT_RADIUS = 8.0;
    private static final double RETURN_ARRIVAL_DIST = 3.0;
    private static final int FUEL_BURN_INTERVAL = 4; // burn 1 fuel every 4 ticks
    private static final double TURN_RADIUS = 2.0; // horizontal turn arc radius in blocks

    public AircraftEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** 工厂方法 — 仅在服务端调用 */
    public static AircraftEntity create(Level level, Player owner, int weaponSlotIndex,
                                        ItemStack aircraftStack,
                                        AircraftAttackMode attackMode,
                                        int coreInventorySlot,
                                        boolean hasBullets,
                                        String payloadType) {
        AircraftEntity entity = new AircraftEntity(ModEntityTypes.AIRCRAFT_ENTITY.get(), level);
        entity.ownerUUID = owner.getUUID();
        entity.weaponSlotIndex = weaponSlotIndex;
        entity.coreInventorySlot = coreInventorySlot;
        entity.attackMode = attackMode;
        entity.hasBullets = hasBullets;
        entity.payloadType = payloadType;

        AircraftInfo info;
        try {
            info = aircraftStack.get(ModDataComponents.AIRCRAFT_INFO.get());
        } catch (Throwable t) {
            // P1-7: 数据组件读取失败
            com.piranport.debug.PiranPortDebug.componentReadFailed(
                    "AIRCRAFT_INFO", aircraftStack, t);
            info = null;
        }
        if (info != null) {
            entity.aircraftType = info.aircraftType();
            entity.panelDamage = ExperienceShellItem.applyAircraftPanelDamageBonus(aircraftStack, info.panelDamage());
            entity.panelSpeed = ExperienceShellItem.applyAircraftPanelSpeedBonus(aircraftStack, info.panelSpeed());
            entity.ammoCapacity = info.ammoCapacity();
            entity.remainingAmmo = info.ammoCapacity();
            entity.fuelCapacity = info.fuelCapacity();
            entity.currentFuel = info.currentFuel();
            entity.bombingMode = info.bombingMode();
        } else {
            // P1-7: 组件缺失（飞机无 AIRCRAFT_INFO）属于异常
            com.piranport.debug.PiranPortDebug.componentReadFailed(
                    "AIRCRAFT_INFO", aircraftStack, "component is null");
        }
        // Default payload based on aircraft type when not explicitly configured
        if (entity.payloadType.isEmpty()) {
            switch (entity.aircraftType) {
                case TORPEDO_BOMBER -> {
                    entity.payloadType = "piranport:aerial_torpedo";
                    entity.hasBullets = false;
                }
                case DIVE_BOMBER, LEVEL_BOMBER -> {
                    entity.payloadType = "piranport:aerial_bomb";
                    entity.hasBullets = false;
                }
                case ASW -> {
                    entity.payloadType = "piranport:depth_charge";
                    entity.hasBullets = false;
                }
                case FIGHTER -> {
                    // Fighters use bullets, no payload
                    entity.hasBullets = true;
                }
                case ROCKET_FIGHTER -> {
                    entity.payloadType = "piranport:rocket_ammo";
                    entity.hasBullets = true;
                }
                default -> {
                    // RECON keeps empty payload and no bullets
                    entity.hasBullets = false;
                }
            }
        }
        entity.aircraftHealth = getMaxHealth(entity.aircraftType);
        entity.originalStack = aircraftStack.copy();
        entity.entityData.set(AIRCRAFT_TYPE_DATA, entity.aircraftType.ordinal());
        entity.entityData.set(OWNER_ID, Optional.of(owner.getUUID()));
        entity.entityData.set(WEAPON_SLOT_INDEX, weaponSlotIndex);

        Vec3 look = owner.getLookAngle();
        // 策划案：飞机在玩家前方 0.3–0.5 格生成，按皮肤微调：航母放飞稍远，潜艇更近
        double spawnDistance = 0.45;
        entity.setPos(owner.getX() + look.x * spawnDistance, owner.getEyeY() - 0.15, owner.getZ() + look.z * spawnDistance);
        entity.orbitAngle = Math.atan2(look.z, look.x);
        entity.stuckCheckPos = entity.position();
        return entity;
    }

    /**
     * Factory for autonomous aircraft (no player owner).
     * Used by debug commands to spawn aircraft from non-player entities (e.g. floating targets).
     */
    public static AircraftEntity createAutonomous(Level level, Vec3 spawnPos, ItemStack aircraftStack, @Nullable LivingEntity target) {
        AircraftEntity entity = new AircraftEntity(ModEntityTypes.AIRCRAFT_ENTITY.get(), level);
        entity.autonomous = true;
        entity.homePosition = spawnPos;
        entity.autonomousTarget = target;

        AircraftInfo info;
        try {
            info = aircraftStack.get(ModDataComponents.AIRCRAFT_INFO.get());
        } catch (Throwable t) {
            com.piranport.debug.PiranPortDebug.componentReadFailed(
                    "AIRCRAFT_INFO", aircraftStack, t);
            info = null;
        }
        if (info != null) {
            entity.aircraftType = info.aircraftType();
            entity.panelDamage = ExperienceShellItem.applyAircraftPanelDamageBonus(aircraftStack, info.panelDamage());
            entity.panelSpeed = ExperienceShellItem.applyAircraftPanelSpeedBonus(aircraftStack, info.panelSpeed());
            entity.ammoCapacity = info.ammoCapacity();
            entity.remainingAmmo = info.ammoCapacity();
            entity.fuelCapacity = info.fuelCapacity();
            entity.currentFuel = info.fuelCapacity();
            entity.bombingMode = info.bombingMode();
        }
        entity.payloadType = "piranport:aerial_bomb";
        entity.aircraftHealth = getMaxHealth(entity.aircraftType);
        entity.originalStack = aircraftStack.copy();
        entity.entityData.set(AIRCRAFT_TYPE_DATA, entity.aircraftType.ordinal());

        entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        entity.orbitAngle = level.random.nextDouble() * Math.PI * 2;
        entity.stuckCheckPos = entity.position();
        return entity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE, FlightState.LAUNCHING.ordinal());
        builder.define(AIRCRAFT_TYPE_DATA, 0);
        builder.define(OWNER_ID, Optional.empty());
        builder.define(WEAPON_SLOT_INDEX, -1);
    }

    /**
     * Override lerpTo to:
     * 1. Smooth position updates on the client via lerp steps (Entity base class sets
     *    position directly in lerpTo, which causes camera stutter since xo==x after
     *    setOldPosAndRot → no partial-tick interpolation).
     * 2. In recon mode, ignore server rotation (client controls camera via mouse).
     *    Uses client recon state instead of getFlightState() to avoid race with entity
     *    data sync — the ReconStatePayload arrives before the STATE data packet.
     */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (level().isClientSide()) {
            // Store lerp target; position will be interpolated in tick()
            this.clientLerpSteps = steps + 2;
            this.clientLerpX = x;
            this.clientLerpY = y;
            this.clientLerpZ = z;
            // In recon mode, keep client-controlled rotation
            if (ClientHooks.isReconEntity(getId())) {
                return;
            }
            // Non-recon: store target rotation for smooth lerping in tick()
            this.clientLerpYRot = yRot;
            this.clientLerpXRot = xRot;
            return;
        }
        super.lerpTo(x, y, z, yRot, xRot, steps);
    }

    @Override public double lerpTargetX() { return clientLerpSteps > 0 ? clientLerpX : getX(); }
    @Override public double lerpTargetY() { return clientLerpSteps > 0 ? clientLerpY : getY(); }
    @Override public double lerpTargetZ() { return clientLerpSteps > 0 ? clientLerpZ : getZ(); }

    @Override
    public void tick() {
        super.tick();

        // Client: position + rotation interpolation + particles
        if (level().isClientSide()) {
            // Process position and rotation lerp steps for smooth rendering
            if (clientLerpSteps > 0) {
                double d = 1.0 / (double) clientLerpSteps;
                double nx = getX() + (clientLerpX - getX()) * d;
                double ny = getY() + (clientLerpY - getY()) * d;
                double nz = getZ() + (clientLerpZ - getZ()) * d;
                setPos(nx, ny, nz);
                // Smooth rotation lerp (shortest-path yaw wrapping)
                float yDiff = clientLerpYRot - getYRot();
                while (yDiff > 180f) yDiff -= 360f;
                while (yDiff < -180f) yDiff += 360f;
                setYRot(getYRot() + yDiff * (float) d);
                setXRot(getXRot() + (clientLerpXRot - getXRot()) * (float) d);
                clientLerpSteps--;
            }
            if (getFlightState() == FlightState.CRUISING && tickCount % 8 == 0) {
                level().addParticle(ParticleTypes.CLOUD, getX(), getY(), getZ(), 0, 0.02, 0);
            }
            return;
        }

        // Index fallback: if onAddedToLevel fired before ownerUUID was populated by
        // readAdditionalSaveData, we'd be missing from AircraftIndex — recover here.
        if (!indexRegistered && ownerUUID != null) {
            com.piranport.aviation.AircraftIndex.add(ownerUUID, this);
            indexRegistered = true;
        }

        FlightState state = getFlightState();
        if (state == FlightState.REMOVED) { discard(); return; }

        Player owner = getOwner();
        if (owner == null) {
            if (autonomous) {
                tickAutonomousServer();
                return;
            }
            // Clean up recon state if owner goes offline
            if (ownerUUID != null && state == FlightState.RECON_ACTIVE) {
                ReconManager.endRecon(ownerUUID);
                releaseAllForcedChunks();
            }
            discard();
            return;
        }

        // Detect state transitions and fire enter/exit hooks
        if (state != lastKnownState) {
            handleStateTransition(lastKnownState, state, owner);
            lastKnownState = state;
        }

        airtimeTicks++;
        stateTicks++;

        // Fuel consumption: burn 1 fuel per FUEL_BURN_INTERVAL ticks during active flight
        if (state == FlightState.CRUISING || state == FlightState.ATTACKING || state == FlightState.RECON_ACTIVE) {
            // 防止燃料容量为0时除零（特殊配置的飞机如侦察机）
            if (fuelCapacity > 0 && currentFuel > 0 && airtimeTicks % FUEL_BURN_INTERVAL == 0) {
                currentFuel--;
            }
            if (currentFuel <= 0) {
                isForcedReturn = true;
                startReturning("fuel_empty");
                if (owner instanceof ServerPlayer sp) {
                    sp.displayClientMessage(
                            Component.translatable("message.piranport.aircraft_no_fuel", getDisplayName()), true);
                }
                return;
            }
        }

        // Defense: 10-min airtime limit
        if (airtimeTicks >= MAX_AIRTIME_TICKS) {
            if (state == FlightState.RETURNING) { recallAndRemove(); return; }
            isForcedReturn = true;
            startReturning("max_airtime");
            return;
        }

        // Defense: stuck detection (disabled in RECON_ACTIVE — player may hover stationary)
        if (state != FlightState.RECON_ACTIVE && stateTicks % STUCK_CHECK_INTERVAL == 0) {
            double moved = position().distanceTo(stuckCheckPos);
            if (moved < STUCK_THRESHOLD) {
                stuckTicks += STUCK_CHECK_INTERVAL;
                if (stuckTicks >= STUCK_CHECK_INTERVAL * 2) { isForcedReturn = true; recallAndRemove(); return; }
            } else {
                stuckTicks = 0;
            }
            stuckCheckPos = position();
        }

        // Defense: distance limit
        // FOLLOW mode uses extended range (recon plane may be 200 blocks away)
        double maxDist = (state == FlightState.RECON_ACTIVE
                || attackMode == AircraftAttackMode.FOLLOW)
                ? getDistanceLimit(MIN_RECON_DIST) : getDistanceLimit(MIN_DIST_FROM_OWNER);
        if (distanceTo(owner) > maxDist) {
            if (state == FlightState.RECON_ACTIVE) {
                // Recon exceeded 200-block range — end recon and return
                isForcedReturn = true;
                startReturning("recon_max_distance");
            } else {
                Vec3 near = owner.position().add(
                        (random.nextDouble() - 0.5) * 4, 3, (random.nextDouble() - 0.5) * 4);
                setPos(near.x, near.y, near.z);
                stuckTicks = 0;
                stuckCheckPos = position();
                isForcedReturn = true;
                startReturning("distance_limit");
            }
            return;
        }

        if (attackCooldown > 0) attackCooldown--;

        switch (state) {
            case LAUNCHING      -> tickLaunching(owner);
            case CRUISING       -> tickCruising(owner);
            case ATTACKING      -> tickAttacking(owner);
            case RETURNING      -> tickReturning(owner);
            case RECON_ACTIVE   -> tickReconActive(owner);
        }

        // ASW sonar scan: every 20 ticks while cruising or attacking
        if (aircraftType == AircraftInfo.AircraftType.ASW
                && (state == FlightState.CRUISING || state == FlightState.ATTACKING)
                && tickCount % 20 == 0) {
            tickAswSonar(owner);
        }

        // === Turn radius constraint + no-hover enforcement ===
        applyTurnRadiusConstraint(state);

        // Update rotation to face movement direction (nose forward)
        Vec3 vel = getDeltaMovement();
        double hDist = vel.horizontalDistance();
        if (hDist > 0.001) {
            float targetYaw = (float) (Math.atan2(-vel.x, vel.z) * (180.0 / Math.PI));
            float targetPitch = (float) (Math.atan2(-vel.y, hDist) * (180.0 / Math.PI));
            setYRot(targetYaw);
            setXRot(targetPitch);
        }

        // Apply movement
        setPos(getX() + vel.x, getY() + vel.y, getZ() + vel.z);
    }

    // ===== State transition hooks =====

    private void handleStateTransition(FlightState from, FlightState to, Player owner) {
        if (to == FlightState.RECON_ACTIVE) {
            ReconManager.startRecon(owner.getUUID(), getUUID());
            // Player body locking is now handled in GameEvents.onPlayerTick
            // Notify client to switch camera
            if (owner instanceof ServerPlayer sp) {
                PacketDistributor.sendToPlayer(sp, new ReconStatePayload(true, getId()));
                com.piranport.PiranPort.LOGGER.info(
                    "Aircraft RECON_START | entityId={} sent ReconStatePayload to player", getId());
            }
        } else if (from == FlightState.RECON_ACTIVE) {
            cleanupReconState(owner);
        }
    }

    /**
     * 统一清理侦察机状态：结束侦察、释放区块、通知客户端。
     * P0修复: 改为public以便在玩家登出时从外部调用
     */
    public void cleanupReconState(Player owner) {
        ReconManager.endRecon(owner.getUUID());
        releaseAllForcedChunks();
        // Notify client to restore camera
        if (owner instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new ReconStatePayload(false, 0));
        }
    }

    // ===== Tick methods =====

    private void tickLaunching(Player owner) {
        double targetY = owner.getY() + CRUISE_ALTITUDE;
        double dy = targetY - getY();
        double rise = Math.min(panelSpeed * 0.3, Math.abs(dy));
        // Horizontal velocity along launch direction (orbitAngle = player's facing at launch)
        // so the aircraft climbs out diagonally instead of rocketing straight up.
        double fwdSpeed = panelSpeed * 0.3;
        double fwdX = Math.cos(orbitAngle) * fwdSpeed;
        double fwdZ = Math.sin(orbitAngle) * fwdSpeed;
        setDeltaMovement(fwdX, dy > 0 ? rise : -rise, fwdZ);
        if (stateTicks >= LAUNCH_DURATION || Math.abs(dy) < 1.5) {
            // RECON goes directly to RECON_ACTIVE; others go to CRUISING
            setState(aircraftType == AircraftInfo.AircraftType.RECON
                    ? FlightState.RECON_ACTIVE : FlightState.CRUISING);
        }
    }

    private void tickCruising(Player owner) {
        boolean isAsw = aircraftType == AircraftInfo.AircraftType.ASW;
        // Transition to ATTACKING if owner has locked targets with at least one alive entity
        if (aircraftType != AircraftInfo.AircraftType.RECON
                && (hasBullets || !payloadType.isEmpty())
                && !FireControlManager.getTargets(owner.getUUID()).isEmpty()) {
            // Verify at least one locked target is still alive before transitioning
            if (hasAliveLockedTarget(owner)) {
                // ASW: only transition if there's an alive ASW-eligible target
                if (isAsw) {
                    if (hasAliveAswLockedTarget(owner)) {
                        hasEverHadFireControl = true;
                        setState(FlightState.ATTACKING);
                        return;
                    }
                    // ASW: alive targets exist but none are aquatic — stay cruising
                } else {
                    // Attack aircraft (payload-only, no bullets) skip airborne targets
                    boolean isAttackAircraft = !hasBullets && !payloadType.isEmpty();
                    if (!isAttackAircraft || hasAliveGroundLockedTarget(owner)) {
                        hasEverHadFireControl = true;
                        setState(FlightState.ATTACKING);
                        return;
                    }
                    // Attack aircraft: alive targets exist but all airborne — stay cruising
                }
            } else {
                // Remove only the dead UUIDs, preserving any newly added targets
                if (owner.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    FireControlManager.removeDeadTargets(owner.getUUID(), targetUuid -> {
                        Entity e = sl.getEntities().get(targetUuid);
                        return e == null || !e.isAlive();
                    });
                }
            }
        }

        // Auto-seek without fire-control locks:
        // Only search ONCE after launch; if FC target died, just keep orbiting.
        // Fighters (hasBullets with empty payload) are included via hasBullets check.
        if ((hasBullets || !payloadType.isEmpty())
                && aircraftType != AircraftInfo.AircraftType.RECON
                && !hasEverHadFireControl && !autoSeekDone
                && FireControlManager.getTargets(owner.getUUID()).isEmpty()) {
            if (autoSeekCooldown > 0) {
                autoSeekCooldown--;
            } else {
                autoSeekDone = true;
                if (level() instanceof ServerLevel sl) {
                    AABB box = getBoundingBox().inflate(32.0);
                    boolean hasNearbyTarget;
                    if (isAsw) {
                        hasNearbyTarget = !sl.getEntitiesOfClass(LivingEntity.class, box,
                                e -> e.isAlive() && e != owner && isAswTarget(e)).isEmpty();
                    } else if (aircraftType == AircraftInfo.AircraftType.FIGHTER
                            || aircraftType == AircraftInfo.AircraftType.ROCKET_FIGHTER) {
                        // Fighters: hostile mobs OR enemy aircraft
                        boolean hasHostile = !sl.getEntitiesOfClass(LivingEntity.class, box,
                                e -> e.isAlive() && e != owner && e instanceof Monster).isEmpty();
                        boolean hasEnemyAircraft = !sl.getEntitiesOfClass(AircraftEntity.class, box,
                                e -> e.isAlive() && e.getOwnerUUID() != null
                                        && !e.getOwnerUUID().equals(owner.getUUID())).isEmpty();
                        hasNearbyTarget = hasHostile || hasEnemyAircraft;
                    } else {
                        hasNearbyTarget = !sl.getEntitiesOfClass(LivingEntity.class, box,
                                e -> e.isAlive() && e != owner && e instanceof Monster && !isAirborneTarget(e)).isEmpty();
                    }
                    if (hasNearbyTarget) {
                        autoSeekCooldown = 0; // reset so resolveTarget() finds it immediately
                        setState(FlightState.ATTACKING);
                        return;
                    }
                }
            }
        }

        // FOLLOW mode: orbit the owner's active recon aircraft, or fall back to player
        if (attackMode == AircraftAttackMode.FOLLOW) {
            // Refresh cache every 20 ticks to avoid per-tick entity lookup
            if (tickCount - reconCacheTick > 20 || cachedReconAircraft != null && cachedReconAircraft.isRemoved()) {
                cachedReconAircraft = findOwnerReconAircraft(owner);
                reconCacheTick = tickCount;
            }
            AircraftEntity reconAircraft = cachedReconAircraft;
            if (reconAircraft != null) {
                orbitAngle += panelSpeed * 0.015;
                double tx = reconAircraft.getX() + Math.cos(orbitAngle) * ORBIT_RADIUS;
                double ty = reconAircraft.getY();  // match recon altitude
                double tz = reconAircraft.getZ() + Math.sin(orbitAngle) * ORBIT_RADIUS;
                Vec3 toTarget = new Vec3(tx - getX(), ty - getY(), tz - getZ());
                double dist = toTarget.length();
                if (dist > 0.1) {
                    setDeltaMovement(toTarget.normalize().scale(Math.min(panelSpeed * 0.35, dist)));
                } else {
                    setDeltaMovement(Vec3.ZERO);
                }
                return;
            }
            // No active recon — fall through to normal orbit around player
        }

        orbitAngle += panelSpeed * 0.015;
        double tx = owner.getX() + Math.cos(orbitAngle) * ORBIT_RADIUS;
        double ty = owner.getY() + CRUISE_ALTITUDE;
        double tz = owner.getZ() + Math.sin(orbitAngle) * ORBIT_RADIUS;
        Vec3 toTarget = new Vec3(tx - getX(), ty - getY(), tz - getZ());
        double dist = toTarget.length();
        if (dist > 0.1) {
            setDeltaMovement(toTarget.normalize().scale(Math.min(panelSpeed * 0.3, dist)));
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
    }

    /** Finds the owner's currently active RECON_ACTIVE aircraft, or null if none. */
    @Nullable
    private AircraftEntity findOwnerReconAircraft(Player owner) {
        if (!(level() instanceof ServerLevel sl)) return null;
        UUID reconEntityId = ReconManager.getReconEntity(owner.getUUID());
        if (reconEntityId == null) return null;
        Entity e = sl.getEntity(reconEntityId);
        if (e instanceof AircraftEntity ae && ae.getFlightState() == FlightState.RECON_ACTIVE) return ae;
        return null;
    }

    // ===== ATTACKING dispatch =====

    private void tickAttacking(Player owner) {
        AircraftCombat.tickAttacking(this, owner);
    }

    private void tickAutonomousAttacking() {
        AircraftCombat.tickAutonomousAttacking(this);
    }

    /**
     * 按目标当前血量与单发伤害决定本次齐射的弹药数。
     * 简化算法：忽略护甲与伤害减免，用 ceil(HP/damage) 保底能击杀，再与剩余弹量取 min，至少 1 发。
     * damagePerShot <= 0 时回退为全投剩余弹药。
     */
    int computeSalvoSize(LivingEntity target, float damagePerShot) {
        if (remainingAmmo <= 0) return 0;
        if (damagePerShot <= 0.01f) return Math.max(1, remainingAmmo);
        float hp = target.getHealth();
        if (hp <= 0f) return 1;
        int needed = (int) Math.ceil(hp / damagePerShot);
        return Math.max(1, Math.min(remainingAmmo, needed));
    }

    /**
     * FIGHTER: hover at ~11 blocks and fire bullets.
     * When fighterAmmoEnabled=false (default), ammo is unlimited and the fighter
     * never returns due to depletion. When true, 64 rounds total (ammoCapacity).
     * Phase 33: target may be a LivingEntity or an enemy AircraftEntity.
     */
    private void tickFighterAttack(@Nullable Player owner, Entity target) {
        AircraftCombat.tickFighterAttack(this, owner, target);
    }

    /**
     * ROCKET_FIGHTER: approach a ground/surface target from altitude, salvo all rockets at once.
     * After hasFired=true, the aircraft falls back to bullets-only fighter logic against air targets.
     */
    private void tickRocketFighterMissileRun(@Nullable Player owner, LivingEntity target) {
        AircraftCombat.tickRocketFighterMissileRun(this, owner, target);
    }

    /**
     * DIVE_BOMBER: climb to target+18 blocks, then commit to a dive with lead prediction.
     * Once the dive is committed, the drop point is fixed — no more recalculation.
     */
    private void tickDiveBomberAttack(@Nullable Player owner, LivingEntity target) {
        AircraftCombat.tickDiveBomberAttack(this, owner, target);
    }

    private void tickTorpedoBomberAttack(@Nullable Player owner, LivingEntity target) {
        AircraftCombat.tickTorpedoBomberAttack(this, owner, target);
    }

    private void tickLevelBomberAttack(@Nullable Player owner, LivingEntity target) {
        AircraftCombat.tickLevelBomberAttack(this, owner, target);
    }

    private LivingEntity resolveTarget(Player owner) { return AircraftCombat.resolveTarget(this, owner); }

    private static boolean isAirborneTarget(Entity e) { return AircraftCombat.isAirborneTarget(e); }

    private Entity resolveFighterTarget(Player owner) { return AircraftCombat.resolveFighterTarget(this, owner); }

    // [combat/target methods moved to AircraftCombat and AircraftAswRecon]


    /**
     * ASW: fly at target+20 altitude, drop depth charges when over target.
     * Similar to level bomber but lower altitude and uses DepthChargeEntity.
     */
    private void tickASWAttack(@Nullable Player owner, LivingEntity target) {
        AircraftAswRecon.tickASWAttack(this, owner, target);
    }

    /**
     * ASW target resolution: only targets submarines and aquatic creatures.
     * 1. FC-locked targets (filtered to ASW-eligible)
     * 2. Auto-seek: underwater monsters and aquatic creatures within 32 blocks.
     */
    @Nullable
    private LivingEntity resolveASWTarget(Player owner) {
        return AircraftAswRecon.resolveASWTarget(this, owner);
    }

    /** Returns true if the entity qualifies as an ASW target (submarine or aquatic creature). */
    private static boolean isAswTarget(Entity e) {
        return AircraftAswRecon.isAswTarget(e);
    }

    /**
     * ASW sonar: scan 16-block radius for underwater entities and send detections to owner.
     * Called from main tick every 20 ticks while ASW aircraft is active (CRUISING or ATTACKING).
     */
    private void tickAswSonar(Player owner) {
        AircraftAswRecon.tickAswSonar(this, owner);
    }

    /**
     * RECON_ACTIVE: read movement input from ReconManager and apply to entity.
     * Maintains chunk forcing around current position (view-distance radius, like a player).
     */
    private void tickReconActive(Player owner) {
        AircraftAswRecon.tickReconActive(this, owner);
    }

    // refreshCachedMapSlots, updateReconChunkLoading, sendPendingChunks
    // moved to AircraftAswRecon

    private void releaseAllForcedChunks() {
        AircraftAswRecon.releaseAllForcedChunks(this);
    }

    // ===== Target resolution =====

    /**
     * Resolves the attack target from the fire control lock list.
     * Attack aircraft (bombers/torpedo bombers) skip airborne targets — only fighters engage air targets.
     */
    @Nullable

    /** Check if any locked target UUID still resolves to an alive entity. */
    private boolean hasAliveLockedTarget(Player owner) {
        if (!(level() instanceof ServerLevel sl)) return false;
        for (UUID uuid : FireControlManager.getTargets(owner.getUUID())) {
            Entity e = sl.getEntity(uuid);
            if (e instanceof LivingEntity le && le.isAlive()) return true;
        }
        return false;
    }

    /** Check if any locked target is alive AND on the ground (not airborne, not underwater). */
    private boolean hasAliveGroundLockedTarget(Player owner) {
        if (!(level() instanceof ServerLevel sl)) return false;
        for (UUID uuid : FireControlManager.getTargets(owner.getUUID())) {
            Entity e = sl.getEntity(uuid);
            if (e instanceof LivingEntity le && le.isAlive()
                    && !isAirborneTarget(le) && !le.isUnderWater()) return true;
        }
        return false;
    }

    /** Check if any locked target is alive AND is an ASW-eligible target. */
    private boolean hasAliveAswLockedTarget(Player owner) {
        if (!(level() instanceof ServerLevel sl)) return false;
        for (UUID uuid : FireControlManager.getTargets(owner.getUUID())) {
            Entity e = sl.getEntity(uuid);
            if (e instanceof LivingEntity le && le.isAlive() && isAswTarget(le)) return true;
        }
        return false;
    }

    /** An entity is considered airborne if it is not on the ground and not in water. */

    private void tickReturning(Player owner) {
        Vec3 toOwner = owner.getEyePosition().subtract(position());
        double dist = toOwner.length();
        if (dist < RETURN_ARRIVAL_DIST) { recallAndRemove(); return; }
        // 返航增速 30%：0.4 × 1.3 = 0.52
        setDeltaMovement(toOwner.normalize().scale(Math.min(panelSpeed * 0.52, dist)));
    }

    // ===== Autonomous (no player owner) tick =====

    private void tickAutonomousServer() {
        FlightState state = getFlightState();
        if (state == FlightState.REMOVED) { discard(); return; }

        airtimeTicks++;
        stateTicks++;

        // Fuel consumption
        if (state == FlightState.CRUISING || state == FlightState.ATTACKING) {
            // 防止燃料容量为0时除零
            if (fuelCapacity > 0 && currentFuel > 0 && airtimeTicks % FUEL_BURN_INTERVAL == 0) currentFuel--;
            if (currentFuel <= 0) { discard(); return; }
        }
        if (airtimeTicks >= MAX_AIRTIME_TICKS) { discard(); return; }
        if (position().distanceTo(homePosition) > getDistanceLimit(MIN_DIST_FROM_OWNER) * 2) { discard(); return; }

        if (attackCooldown > 0) attackCooldown--;

        switch (state) {
            case LAUNCHING -> {
                double targetY = homePosition.y + CRUISE_ALTITUDE;
                double dy = targetY - getY();
                double rise = Math.min(panelSpeed * 0.3, Math.abs(dy));
                setDeltaMovement(getDeltaMovement().x * 0.5, dy > 0 ? rise : -rise, getDeltaMovement().z * 0.5);
                if (stateTicks >= LAUNCH_DURATION || Math.abs(dy) < 1.5) {
                    setState(FlightState.CRUISING);
                }
            }
            case CRUISING -> {
                // Auto-seek players
                if (level() instanceof ServerLevel sl && !payloadType.isEmpty()) {
                    AABB box = getBoundingBox().inflate(48.0);
                    boolean hasTarget = !sl.getEntitiesOfClass(Player.class, box,
                            e -> e.isAlive() && !e.isSpectator()).isEmpty();
                    if (hasTarget) {
                        setState(FlightState.ATTACKING);
                        return;
                    }
                }
                // Orbit home position
                orbitAngle += panelSpeed * 0.015;
                double tx = homePosition.x + Math.cos(orbitAngle) * ORBIT_RADIUS;
                double ty = homePosition.y + CRUISE_ALTITUDE;
                double tz = homePosition.z + Math.sin(orbitAngle) * ORBIT_RADIUS;
                Vec3 toTarget = new Vec3(tx - getX(), ty - getY(), tz - getZ());
                double dist = toTarget.length();
                if (dist > 0.1) {
                    setDeltaMovement(toTarget.normalize().scale(Math.min(panelSpeed * 0.3, dist)));
                } else {
                    setDeltaMovement(Vec3.ZERO);
                }
            }
            case ATTACKING -> {
                if (remainingAmmo <= 0) { discard(); return; }
                if (!(level() instanceof ServerLevel sl)) return;
                AABB box = getBoundingBox().inflate(48.0);
                LivingEntity target = sl.getEntitiesOfClass(Player.class, box,
                                e -> e.isAlive() && !e.isSpectator())
                        .stream().map(e -> (LivingEntity) e)
                        .min(Comparator.comparingDouble(this::distanceTo)).orElse(null);
                if (target == null) { setState(FlightState.CRUISING); return; }
                tickAutonomousLevelBomb(target);
            }
            case RETURNING -> discard();
            default -> {}
        }

        // === Turn radius constraint + no-hover enforcement ===
        applyTurnRadiusConstraint(state);

        // Update rotation to face movement direction
        Vec3 vel = getDeltaMovement();
        double hDist = vel.horizontalDistance();
        if (hDist > 0.001) {
            setYRot((float) (Math.atan2(-vel.x, vel.z) * (180.0 / Math.PI)));
            setXRot((float) (Math.atan2(-vel.y, hDist) * (180.0 / Math.PI)));
        }

        // Apply movement (autonomous path — main tick's setPos is not reached)
        setPos(getX() + vel.x, getY() + vel.y, getZ() + vel.z);
    }

    private void tickAutonomousLevelBomb(LivingEntity target) {
        double bombAltitude = target.getY() + 32.0;

        // Phase 1: Climb to bombing altitude
        if (getY() < bombAltitude - 1.5) {
            levelBombDropped = false;
            levelRunDirection = null;
            levelDropPoint = null;
            levelRunTicks = 0;
            Vec3 toPoint = new Vec3(target.getX() - getX(), bombAltitude - getY(), target.getZ() - getZ());
            double dist = toPoint.length();
            setDeltaMovement(toPoint.normalize().scale(Math.min(panelSpeed * 0.4, dist)));
            return;
        }

        // Establish run direction once at altitude
        if (levelRunDirection == null) {
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            double horizDist = Math.sqrt(dx * dx + dz * dz);
            if (horizDist < 0.1) {
                levelRunDirection = new Vec3(1, 0, 0);
            } else {
                levelRunDirection = new Vec3(dx / horizDist, 0, dz / horizDist);
            }
            levelBombDropped = false;
            levelDropPoint = null;
            levelRunTicks = 0;
        }

        // Phase 2: Fly along the run direction
        double yCorrect = (bombAltitude - getY()) * 0.15;
        setDeltaMovement(levelRunDirection.x * panelSpeed * 0.4, yCorrect, levelRunDirection.z * panelSpeed * 0.4);
        levelRunTicks++;

        // Fail-safe: abandon a run that drags on forever (autonomous bombers self-destruct on return)
        if (!levelBombDropped && levelRunTicks > 200) {
            discard();
            return;
        }

        // Drop bomb when over target, or when we just overflew it without a drop
        if (!levelBombDropped) {
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            double horizDist = Math.sqrt(dx * dx + dz * dz);
            double signedProj = dx * levelRunDirection.x + dz * levelRunDirection.z;
            boolean atDropWindow = horizDist < 3.0;
            double stepLen = panelSpeed * 0.4;
            boolean flewPastButClose = signedProj <= 0 && horizDist < Math.max(stepLen + 2.0, 6.0);
            if (atDropWindow || flewPastButClose) {
                float bombDamage = panelDamage * 1.5f;
                int toFire = computeSalvoSize(target, bombDamage);
                for (int i = 0; i < toFire; i++) {
                    double offset = (i - (toFire - 1) / 2.0) * 0.8;
                    double spawnX = getX() + levelRunDirection.x * offset;
                    double spawnZ = getZ() + levelRunDirection.z * offset;
                    AerialBombEntity bomb = new AerialBombEntity(level(), bombDamage, 2.5f);
                    bomb.moveTo(spawnX, getY(), spawnZ, bomb.getYRot(), bomb.getXRot());
                    bomb.setDeltaMovement(getDeltaMovement().x * 0.1, -0.1, getDeltaMovement().z * 0.1);
                    bomb.setSourceAircraftName(getDisplayName());
                    bomb.setSourceAircraft(this);
                    if (getOwner() != null) bomb.setOwner(getOwner());
                    else if (autonomous) bomb.setOwner(this);
                    level().addFreshEntity(bomb);
                }
                remainingAmmo -= toFire;
                levelBombDropped = true;
                levelDropPoint = position();
            } else if (signedProj <= 0) {
                // Flew past target but still out of drop range — re-plan next tick
                levelRunDirection = null;
                return;
            }
        }

        // Phase 3: After dropping, fly 10 blocks past then reassess
        if (levelBombDropped && levelDropPoint != null) {
            double distFromDrop = position().subtract(levelDropPoint).horizontalDistance();
            if (distFromDrop >= 10.0) {
                levelRunDirection = null;
                levelDropPoint = null;
                levelBombDropped = false;
                levelRunTicks = 0;
            }
        }
    }

    // ===== Turn radius constraint =====

    /**
     * Enforces a circular arc turn with radius {@link #TURN_RADIUS} and prevents hovering.
     * Called after each state's tick method sets the desired delta movement, and before
     * rotation/position update.
     */
    private void applyTurnRadiusConstraint(FlightState state) {
        Vec3 rawVel = getDeltaMovement();

        // Enforce minimum horizontal speed (no hovering) — skip during LAUNCHING (vertical ascent)
        if (state != FlightState.LAUNCHING && lastHorizontalDir != null) {
            double rawHorizSpeed = rawVel.horizontalDistance();
            double minSpeed = panelSpeed * 0.05;
            if (rawHorizSpeed < minSpeed) {
                rawVel = new Vec3(lastHorizontalDir.x * minSpeed, rawVel.y, lastHorizontalDir.z * minSpeed);
                setDeltaMovement(rawVel);
            }
        }

        // Apply turn radius constraint
        Vec3 constrained = constrainTurnRadius(getDeltaMovement());
        setDeltaMovement(constrained);

        // Update stored horizontal direction for next tick
        double hd = constrained.horizontalDistance();
        if (hd > 0.001) {
            lastHorizontalDir = new Vec3(constrained.x / hd, 0, constrained.z / hd);
        }
    }

    /**
     * Limits horizontal direction change to the maximum angular rate allowed by
     * a circular arc of radius {@link #TURN_RADIUS}.
     * Vertical (Y) component is passed through unchanged — altitude changes are not arc-limited.
     */
    private Vec3 constrainTurnRadius(Vec3 desiredVel) {
        double desiredHorizSpeed = desiredVel.horizontalDistance();

        // Too slow or no prior direction — allow free movement
        if (desiredHorizSpeed < 0.001 || lastHorizontalDir == null) {
            return desiredVel;
        }

        // Max yaw change per tick:  ω = v / r  (radians)
        double maxAngle = desiredHorizSpeed / TURN_RADIUS;

        // Desired horizontal direction
        double invSpeed = 1.0 / desiredHorizSpeed;
        double desiredDirX = desiredVel.x * invSpeed;
        double desiredDirZ = desiredVel.z * invSpeed;

        // Angle between last direction and desired direction
        double dot = lastHorizontalDir.x * desiredDirX + lastHorizontalDir.z * desiredDirZ;
        dot = Math.max(-1.0, Math.min(1.0, dot));
        double angleDiff = Math.acos(dot);

        // Within turn rate — no constraint needed
        if (angleDiff <= maxAngle) {
            return desiredVel;
        }

        // Determine turn direction via 2D cross product (positive = turn left / CCW)
        double cross = lastHorizontalDir.x * desiredDirZ - lastHorizontalDir.z * desiredDirX;
        double currentYawRad = Math.atan2(-lastHorizontalDir.x, lastHorizontalDir.z);
        currentYawRad += (cross >= 0 ? maxAngle : -maxAngle);

        double newX = -Math.sin(currentYawRad) * desiredHorizSpeed;
        double newZ = Math.cos(currentYawRad) * desiredHorizSpeed;

        return new Vec3(newX, desiredVel.y, newZ);
    }

    /**
     * Begin returning to owner — enters RETURNING state so the aircraft visibly
     * flies back before being recalled.  Safe to call from any state; silently
     * ignored if already RETURNING or REMOVED.
     *
     * @param reason debug reason tag (logged when debug mode is active)
     */
    public void startReturning(String reason) {
        FlightState cur = getFlightState();
        if (cur == FlightState.RETURNING || cur == FlightState.REMOVED) return;
        // P0-3: 返航触发埋点（带原因/状态/位置/玩家）
        String posStr = String.format("[%d,%d,%d]",
                blockPosition().getX(), blockPosition().getY(), blockPosition().getZ());
        com.piranport.debug.PiranPortDebug.aircraftReturnTriggered(
                getId(), reason, getFlightState(), posStr);
        com.piranport.debug.PiranPortDebug.event(
                "Aircraft RETURNING | type={} entityId={} reason={}",
                aircraftType.name(), getId(), reason);
        setState(FlightState.RETURNING);
    }

    /** Remove entity and return aircraft item to the owner's ship core weapon slot. */
    public void recallAndRemove() {
        com.piranport.debug.PiranPortDebug.event(
                "Aircraft RETURN | type={} entityId={} forced={}",
                aircraftType.name(), getId(), isForcedReturn);
        boolean returned = false;
        if (!level().isClientSide() && ownerUUID != null && level() instanceof ServerLevel sl) {
            Player owner = sl.getServer().getPlayerList().getPlayer(ownerUUID);
            if (owner != null) {
                // Clean up recon if active
                if (getFlightState() == FlightState.RECON_ACTIVE) {
                    cleanupReconState(owner);
                }
                // Action bar notification
                Component aircraftName = buildReturnStack().getHoverName();
                String msgKey = isForcedReturn
                        ? "message.piranport.aircraft_lost"
                        : "message.piranport.aircraft_returned";
                owner.displayClientMessage(Component.translatable(msgKey, aircraftName), true);
                returned = returnItemToOwner(owner);
            } else {
                // Owner offline — still clean up recon state and chunks
                ReconManager.endRecon(ownerUUID);
                releaseAllForcedChunks();
                lastReturnResult = "OWNER_OFFLINE";
            }
        }
        // P0-3: 实体移除埋点（带回程是否成功、存活时长）
        long lifetimeSec = (System.currentTimeMillis() - spawnTimeMs) / 1000L;
        com.piranport.debug.PiranPortDebug.aircraftRemoved(
                getId(), ownerUUID, returned, lifetimeSec);
        discard();
    }

    private boolean returnItemToOwner(Player player) {
        if (autonomous) {
            // 自主飞机不返回物品栏，直接掉落物品
            Block.popResource(player.level(), player.blockPosition(), buildReturnStack());
            lastReturnResult = "DROPPED";
            com.piranport.debug.PiranPortDebug.aircraftReturnItem(
                    getId(), ownerUUID, true, weaponSlotIndex, "DROPPED");
            return true;
        }

        ItemStack returnStack = buildReturnStack();
        // 直接尝试放回原武器槽：若空则成功，否则原版 placeItemBackInInventory 会自动入库/掉落
        if (weaponSlotIndex == 40) {
            if (player.getInventory().offhand.get(0).isEmpty()) {
                player.getInventory().offhand.set(0, returnStack);
                lastReturnResult = "SUCCESS";
                com.piranport.debug.PiranPortDebug.aircraftReturnItem(
                        getId(), ownerUUID, true, weaponSlotIndex, "SUCCESS");
                return true;
            }
        } else if (weaponSlotIndex >= 0 && weaponSlotIndex < player.getInventory().items.size()
                && player.getInventory().items.get(weaponSlotIndex).isEmpty()) {
            player.getInventory().items.set(weaponSlotIndex, returnStack);
            lastReturnResult = "SUCCESS";
            com.piranport.debug.PiranPortDebug.aircraftReturnItem(
                    getId(), ownerUUID, true, weaponSlotIndex, "SUCCESS");
            return true;
        }
        // 槽位已满：placeItemBackInInventory 返回 void，要么合并要么掉落
        // 通过调用前后槽位变化检测是否合并成功
        int beforeCount = player.getInventory().items.stream()
                .mapToInt(s -> s.isEmpty() ? 0 : s.getCount()).sum();
        player.getInventory().placeItemBackInInventory(returnStack);
        int afterCount = player.getInventory().items.stream()
                .mapToInt(s -> s.isEmpty() ? 0 : s.getCount()).sum();
        String result = afterCount > beforeCount ? "MERGED" : "SLOT_FULL";
        lastReturnResult = result;
        com.piranport.debug.PiranPortDebug.aircraftReturnItem(
                getId(), ownerUUID, true, weaponSlotIndex, result);
        return afterCount > beforeCount;
    }

    private ItemStack buildReturnStack() {
        // Restore original ItemStack if available, preserving custom names/enchants/components
        ItemStack stack;
        if (originalStack != null && !originalStack.isEmpty()) {
            stack = originalStack.copy();
        } else {
            stack = switch (aircraftType) {
                case FIGHTER        -> new ItemStack(ModItems.FIGHTER_SQUADRON.get());
                case DIVE_BOMBER    -> new ItemStack(ModItems.DIVE_BOMBER_SQUADRON.get());
                case TORPEDO_BOMBER -> new ItemStack(ModItems.SWORDFISH_TORPEDO.get());
                case LEVEL_BOMBER   -> new ItemStack(ModItems.B25_BOMBER.get());
                case ASW            -> new ItemStack(ModItems.SWORDFISH_ASW.get());
                case RECON          -> new ItemStack(ModItems.RECON_SQUADRON.get());
                case ROCKET_FIGHTER -> new ItemStack(ModItems.F6F_HELLCAT_ROCKET.get());
            };
        }
        // Intentionally reset fuel to 0: returning aircraft must be refueled before next sortie
        AircraftInfo info = stack.get(ModDataComponents.AIRCRAFT_INFO.get());
        if (info != null) stack.set(ModDataComponents.AIRCRAFT_INFO.get(), info.withCurrentFuel(0));
        return stack;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide() || ownerUUID == null || !ownerUUID.equals(player.getUUID()))
            return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // Manual fuel resupply: hold aviation_fuel → right-click
        if (held.is(ModItems.AVIATION_FUEL.get())) {
            if (currentFuel < fuelCapacity) {
                held.shrink(1);
                currentFuel = fuelCapacity;
                player.displayClientMessage(
                        Component.translatable("message.piranport.aircraft_refueled", getDisplayName()), true);
                return InteractionResult.sidedSuccess(false);
            }
            // Already full fuel — fall through to recall check
        }

        // Manual ammo resupply: hold the aircraft's payload item → right-click
        if (!held.isEmpty() && !payloadType.isEmpty()) {
            String heldId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
            if (payloadType.equals(heldId) && remainingAmmo < ammoCapacity) {
                int needed = ammoCapacity - remainingAmmo;
                int toLoad = Math.min(needed, held.getCount());
                held.shrink(toLoad);
                remainingAmmo += toLoad;
                hasFired = false;
                player.displayClientMessage(
                        Component.translatable("message.piranport.aircraft_resupplied", getDisplayName()), true);
                return InteractionResult.sidedSuccess(false);
            }
        }

        // Empty hand → fly back then recall
        if (held.isEmpty()) {
            startReturning("player_interact");
            return InteractionResult.sidedSuccess(false);
        }

        return InteractionResult.PASS;
    }

    /**
     * Attempt to draw payload items from the owner's inventory. Returns true if successful.
     */
    /** Lazily resolve the payload type string to an Item reference for fast comparison. */
    private net.minecraft.world.item.Item resolvePayloadItem() {
        if (cachedPayloadItem == null && !payloadType.isEmpty()) {
            net.minecraft.resources.ResourceLocation loc =
                    net.minecraft.resources.ResourceLocation.tryParse(payloadType);
            cachedPayloadItem = loc != null ? BuiltInRegistries.ITEM.get(loc) : null;
        }
        return cachedPayloadItem;
    }

    boolean tryAutoResupplyAmmo(Player owner) {
        if (payloadType.isEmpty()) return false;
        net.minecraft.world.item.Item payloadItem = resolvePayloadItem();
        if (payloadItem == null) return false;

        int needed = ammoCapacity - remainingAmmo;
        int loaded = 0;
        for (ItemStack ammo : owner.getInventory().items) {
            if (needed <= 0) break;
            if (!ammo.isEmpty() && ammo.getItem() == payloadItem) {
                int take = Math.min(needed, ammo.getCount());
                ammo.shrink(take);
                needed -= take;
                loaded += take;
            }
        }
        ItemStack offhand = owner.getInventory().offhand.get(0);
        if (needed > 0 && !offhand.isEmpty() && offhand.getItem() == payloadItem) {
            int take = Math.min(needed, offhand.getCount());
            offhand.shrink(take);
            loaded += take;
        }
        if (loaded > 0) {
            remainingAmmo += loaded;
            hasFired = false;
            return true;
        }
        return false;
    }

    /**
     * Attempt to draw one aviation_fuel from the owner's inventory.
     * Returns true if successful.
     */
    private boolean tryAutoResupplyFuel(Player owner) {
        for (ItemStack ammo : owner.getInventory().items) {
            if (ammo.is(ModItems.AVIATION_FUEL.get()) && ammo.getCount() > 0) {
                ammo.shrink(1);
                currentFuel = fuelCapacity;
                return true;
            }
        }
        ItemStack offhand = owner.getInventory().offhand.get(0);
        if (offhand.is(ModItems.AVIATION_FUEL.get()) && offhand.getCount() > 0) {
            offhand.shrink(1);
            currentFuel = fuelCapacity;
            return true;
        }
        return false;
    }

    // ===== Phase 33: air combat =====

    private static int getMaxHealth(AircraftInfo.AircraftType type) {
        return switch (type) {
            case FIGHTER        -> 20;
            case DIVE_BOMBER    -> 15;
            case TORPEDO_BOMBER -> 15;
            case LEVEL_BOMBER   -> 12;
            case ASW            -> 12;
            case RECON          -> 10;
            case ROCKET_FIGHTER -> 20;
        };
    }

    /**
     * Aircraft takes damage from bullets and other sources.
     * Friendly fire (same ownerUUID) is ignored.
     * On death: explosion effect, discard WITHOUT returning item to owner.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide()) return false;
        FlightState state = getFlightState();
        if (state == FlightState.REMOVED) return false;

        // Friendly fire — ignore all damage from the same player's weapons/aircraft
        Entity attacker = source.getEntity();
        if (ownerUUID != null) {
            if (attacker instanceof Player p && ownerUUID.equals(p.getUUID())) return false;
            if (attacker instanceof AircraftEntity ac && ownerUUID.equals(ac.getOwnerUUID())) return false;
        }
        // Autonomous fratricide: damage from another autonomous aircraft's projectile explosion
        if (autonomous && attacker instanceof AircraftEntity ac && ac.isAutonomous() && ac != this) {
            return false;
        }

        aircraftHealth -= (int) Math.ceil(amount);

        // Hit sound
        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.IRON_GOLEM_HURT, SoundSource.HOSTILE,
                0.4f, 1.4f + random.nextFloat() * 0.2f);

        if (aircraftHealth <= 0) {
            // Explosion effect + destroy (no item return)
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0f, 0.9f + random.nextFloat() * 0.2f);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(),
                        1, 0.3, 0.3, 0.3, 0.0);
            }
            // Clean up recon state if needed + notify owner
            Player owner = getOwner();
            if (state == FlightState.RECON_ACTIVE && ownerUUID != null) {
                if (owner != null) cleanupReconState(owner);
                else { ReconManager.endRecon(ownerUUID); releaseAllForcedChunks(); }
            }
            // Action bar shot-down notification
            if (owner != null) {
                Component aircraftName = buildReturnStack().getHoverName();
                if (attacker != null) {
                    owner.displayClientMessage(Component.translatable(
                            "message.piranport.aircraft_shot_down_by", aircraftName, attacker.getDisplayName()), true);
                } else {
                    owner.displayClientMessage(Component.translatable(
                            "message.piranport.aircraft_shot_down", aircraftName), true);
                }
            }
            com.piranport.debug.PiranPortDebug.event(
                    "Aircraft KILLED | type={} entityId={} attacker={}",
                    aircraftType.name(), getId(),
                    attacker != null ? attacker.getType().toShortString() : "unknown");
            // 掉落飞机物品（防爆+浮水）
            ItemStack dropStack = buildReturnStack();
            if (!dropStack.isEmpty()) {
                AircraftDropEntity drop = new AircraftDropEntity(
                        level(), getX(), getY(), getZ(), dropStack);
                level().addFreshEntity(drop);
            }
            discard();
        }
        return true;
    }

    /**
     * Phase 33: fighter target resolution with extended priority:
     * 1. Fire control locked targets (any entity type)
     * 2. Enemy aircraft (non-same-owner AircraftEntity, 32-block radius)
     * 3. Hostile mobs (32-block radius)
     *
     * When fighter-air-only mode is active, only FC-locked airborne targets are considered.
     */
    @Nullable

    public void setState(FlightState newState) {
        if (newState == FlightState.ATTACKING) {
            hasFired = false;
            attackCooldown = 0;
            diveCommitted = false;
            diveTarget = null;
        }
        entityData.set(STATE, newState.ordinal());
        stateTicks = 0;
    }

    private static final FlightState[] FLIGHT_STATE_VALUES = FlightState.values();
    private static final AircraftInfo.AircraftType[] AIRCRAFT_TYPE_VALUES = AircraftInfo.AircraftType.values();

    /** Safely maps persisted/synced ordinals back to the enum, preserving save compatibility. */
    private static FlightState stateByOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < FLIGHT_STATE_VALUES.length
                ? FLIGHT_STATE_VALUES[ordinal] : FlightState.REMOVED;
    }

    public FlightState getFlightState() {
        return stateByOrdinal(entityData.get(STATE));
    }

    public AircraftInfo.AircraftType getAircraftType() {
        int ordinal = entityData.get(AIRCRAFT_TYPE_DATA);
        if (ordinal < 0 || ordinal >= AIRCRAFT_TYPE_VALUES.length) return AircraftInfo.AircraftType.FIGHTER;
        return AIRCRAFT_TYPE_VALUES[ordinal];
    }

    /** Returns true if this aircraft is owned by the given player. Works client-side (synced). */
    public boolean isOwnedByPlayer(Player player) {
        Optional<UUID> id = entityData.get(OWNER_ID);
        return id.isPresent() && id.get().equals(player.getUUID());
    }

    @Nullable
    public UUID getOwnerUUID() { return ownerUUID; }

    public boolean isAutonomous() { return autonomous; }

    /**
     * Returns the player owner of this aircraft, or null if offline or autonomous.
     * NOTE: server-side only — always returns null on the client side. Client code
     * that needs to know the owner UUID should use {@link #getOwnerUUID()} instead.
     */
    @Nullable
    public Player getOwner() {
        if (ownerUUID == null) return null;
        if (level() instanceof ServerLevel sl) {
            return sl.getServer().getPlayerList().getPlayer(ownerUUID);
        }
        return null;
    }

    @Override
    public boolean isCurrentlyGlowing() {
        // Vanilla glow has highest priority
        if (super.isCurrentlyGlowing()) return true;
        if (level().isClientSide()) {
            return ClientHooks.shouldAircraftGlow(this);
        }
        return false;
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable("entity.piranport.aircraft." + aircraftType.getSerializedName());
    }

    @Override
    public int getTeamColor() {
        if (level().isClientSide()) {
            // Vanilla glow has highest priority for color (unconditional)
            if (super.isCurrentlyGlowing()) {
                return super.getTeamColor();
            }
            return ClientHooks.getAircraftGlowColor(this, super.getTeamColor());
        }
        return super.getTeamColor();
    }

    @Override
    public boolean isPickable() { return true; }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide()) {
            // Safety net: release any forced chunks that weren't cleaned up
            releaseAllForcedChunks();
            // Clean up recon state
            if (ownerUUID != null) {
                ReconManager.endRecon(ownerUUID);
                // Remove slowness if we applied it
                if (appliedSlowness && level() instanceof ServerLevel sl) {
                    Player owner = sl.getServer().getPlayerList().getPlayer(ownerUUID);
                    if (owner != null) {
                        owner.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    }
                    appliedSlowness = false;
                }
            }
        }
        super.remove(reason);
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide() && ownerUUID != null) {
            com.piranport.aviation.AircraftIndex.add(ownerUUID, this);
            indexRegistered = true;
        }
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        if (!level().isClientSide() && ownerUUID != null) {
            com.piranport.aviation.AircraftIndex.remove(ownerUUID, this);
            indexRegistered = false;
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) ownerUUID = tag.getUUID("OwnerUUID");
        weaponSlotIndex = tag.contains("WeaponSlot") ? tag.getInt("WeaponSlot") : -1;
        if (!level().isClientSide) {
            entityData.set(WEAPON_SLOT_INDEX, weaponSlotIndex);
        }
        coreInventorySlot = tag.getInt("CoreSlot");
        try { aircraftType = AircraftInfo.AircraftType.valueOf(tag.getString("AircraftType")); }
        catch (IllegalArgumentException e) { aircraftType = AircraftInfo.AircraftType.FIGHTER; }
        try { attackMode = AircraftAttackMode.valueOf(tag.getString("AttackMode")); }
        catch (IllegalArgumentException e) { attackMode = AircraftAttackMode.FOCUS; }
        panelDamage = tag.getFloat("PanelDamage");
        panelSpeed  = tag.getFloat("PanelSpeed");
        remainingAmmo = tag.getInt("RemainingAmmo");
        ammoCapacity = tag.getInt("AmmoCapacity");
        currentFuel = tag.getInt("CurrentFuel");
        fuelCapacity = tag.getInt("FuelCapacity");
        if (fuelCapacity <= 0) fuelCapacity = 1; // guard against old saves
        hasBullets = tag.getBoolean("HasBullets");
        payloadType = tag.getString("PayloadType");
        try { bombingMode = AircraftInfo.BombingMode.valueOf(tag.getString("BombingMode")); }
        catch (IllegalArgumentException e) { bombingMode = AircraftInfo.BombingMode.DIVE; }
        lastForcedChunkX = tag.getInt("LastForcedChunkX");
        lastForcedChunkZ = tag.getInt("LastForcedChunkZ");
        if (!tag.contains("LastForcedChunkX")) lastForcedChunkX = Integer.MIN_VALUE;
        // Release any forced chunks from the previous session and intentionally do NOT
        // restore them into reconForcedChunks. Recon resumes from scratch on next tick:
        // updateReconForcedChunks() will recompute the desired set and re-force the chunks
        // around the aircraft's current position. If we restored the saved set here, those
        // entries would be desynced from the (now-released) world state and the eviction
        // diff in updateReconForcedChunks would leak chunks.
        if (tag.contains("ReconForcedChunks") && level() instanceof ServerLevel sl) {
            long[] saved = tag.getLongArray("ReconForcedChunks");
            for (long key : saved) {
                sl.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),
                        net.minecraft.world.level.ChunkPos.getZ(key), false);
            }
        }
        reconForcedChunks.clear();
        int savedOrdinal = tag.contains("FlightState") ? tag.getInt("FlightState") : 0;
        FlightState savedState = stateByOrdinal(savedOrdinal);
        if (savedState == FlightState.REMOVED && savedOrdinal != FlightState.REMOVED.ordinal()) {
            PiranPort.LOGGER.warn("Invalid FlightState ordinal {} for aircraft {}; falling back to {}",
                    savedOrdinal, getId(), savedState.name());
        }
        entityData.set(STATE, savedState.ordinal());
        entityData.set(AIRCRAFT_TYPE_DATA, aircraftType.ordinal());
        if (ownerUUID != null) entityData.set(OWNER_ID, Optional.of(ownerUUID));

        // P1修复: 如果重新加载后状态是RECON_ACTIVE但玩家不在线，强制切换到RETURNING
        if (getFlightState() == FlightState.RECON_ACTIVE && level() instanceof ServerLevel sl) {
            if (ownerUUID != null) {
                ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(ownerUUID);
                if (owner == null) {
                    // 玩家不在线，强制切换到返航状态
                    setState(FlightState.RETURNING);
                    com.piranport.PiranPort.LOGGER.info("Aircraft {} switched from RECON_ACTIVE to RETURNING (owner offline)", getId());
                }
            }
        }

        airtimeTicks = tag.getInt("AirtimeTicks");
        hasFired = tag.getBoolean("HasFired");
        aircraftHealth = tag.contains("AircraftHealth")
                ? tag.getInt("AircraftHealth")
                : getMaxHealth(aircraftType);
        if (tag.contains("OriginalStack")) {
            originalStack = ItemStack.parse(level().registryAccess(), tag.getCompound("OriginalStack"))
                    .orElse(ItemStack.EMPTY);
        }
        autonomous = tag.getBoolean("Autonomous");
        if (tag.contains("HomeX")) {
            homePosition = new Vec3(tag.getDouble("HomeX"), tag.getDouble("HomeY"), tag.getDouble("HomeZ"));
        }
        stateTicks = tag.getInt("StateTicks");
        attackCooldown = tag.getInt("AttackCooldown");
        autoSeekDone = tag.getBoolean("AutoSeekDone");
        hasEverHadFireControl = tag.getBoolean("HasEverHadFireControl");
        diveCommitted = tag.getBoolean("DiveCommitted");
        if (tag.contains("DiveTargetX")) {
            diveTarget = new Vec3(tag.getDouble("DiveTargetX"), tag.getDouble("DiveTargetY"), tag.getDouble("DiveTargetZ"));
        }
        levelBombDropped = tag.getBoolean("LevelBombDropped");
        if (tag.contains("LevelRunDirX")) {
            levelRunDirection = new Vec3(tag.getDouble("LevelRunDirX"), tag.getDouble("LevelRunDirY"), tag.getDouble("LevelRunDirZ"));
        }
        if (tag.contains("LevelDropPointX")) {
            levelDropPoint = new Vec3(tag.getDouble("LevelDropPointX"), tag.getDouble("LevelDropPointY"), tag.getDouble("LevelDropPointZ"));
        }
        appliedSlowness = tag.getBoolean("AppliedSlowness");
        if (tag.contains("LastKnownState")) {
            try { lastKnownState = FlightState.valueOf(tag.getString("LastKnownState")); }
            catch (IllegalArgumentException e) { lastKnownState = FlightState.LAUNCHING; }
        }
        isForcedReturn = tag.getBoolean("IsForcedReturn");
        if (tag.contains("OrbitAngle")) orbitAngle = tag.getDouble("OrbitAngle");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUUID != null) tag.putUUID("OwnerUUID", ownerUUID);
        tag.putInt("WeaponSlot", weaponSlotIndex);
        tag.putInt("CoreSlot", coreInventorySlot);
        tag.putString("AircraftType", aircraftType.name());
        tag.putString("AttackMode", attackMode.name());
        tag.putFloat("PanelDamage", panelDamage);
        tag.putInt("AmmoCapacity", ammoCapacity);
        tag.putInt("CurrentFuel", currentFuel);
        tag.putInt("FuelCapacity", fuelCapacity);
        tag.putBoolean("HasBullets", hasBullets);
        tag.putString("PayloadType", payloadType);
        tag.putString("BombingMode", bombingMode.name());
        tag.putFloat("PanelSpeed", panelSpeed);
        tag.putInt("RemainingAmmo", remainingAmmo);
        tag.putInt("LastForcedChunkX", lastForcedChunkX);
        tag.putInt("LastForcedChunkZ", lastForcedChunkZ);
        // Persist forced chunks so they can be released after server restart
        if (!reconForcedChunks.isEmpty()) {
            long[] chunks = new long[reconForcedChunks.size()];
            int idx = 0;
            for (long key : reconForcedChunks) chunks[idx++] = key;
            tag.putLongArray("ReconForcedChunks", chunks);
        }
        tag.putInt("FlightState", entityData.get(STATE));
        tag.putInt("AirtimeTicks", airtimeTicks);
        tag.putBoolean("HasFired", hasFired);
        tag.putInt("AircraftHealth", aircraftHealth);
        if (originalStack != null && !originalStack.isEmpty()) {
            tag.put("OriginalStack", originalStack.save(level().registryAccess()));
        }
        tag.putBoolean("Autonomous", autonomous);
        if (autonomous) {
            tag.putDouble("HomeX", homePosition.x);
            tag.putDouble("HomeY", homePosition.y);
            tag.putDouble("HomeZ", homePosition.z);
        }
        // Runtime-but-must-survive-chunk-unload fields
        tag.putInt("StateTicks", stateTicks);
        tag.putInt("AttackCooldown", attackCooldown);
        tag.putBoolean("AutoSeekDone", autoSeekDone);
        tag.putBoolean("HasEverHadFireControl", hasEverHadFireControl);
        tag.putBoolean("DiveCommitted", diveCommitted);
        if (diveTarget != null) {
            tag.putDouble("DiveTargetX", diveTarget.x);
            tag.putDouble("DiveTargetY", diveTarget.y);
            tag.putDouble("DiveTargetZ", diveTarget.z);
        }
        tag.putBoolean("LevelBombDropped", levelBombDropped);
        if (levelRunDirection != null) {
            tag.putDouble("LevelRunDirX", levelRunDirection.x);
            tag.putDouble("LevelRunDirY", levelRunDirection.y);
            tag.putDouble("LevelRunDirZ", levelRunDirection.z);
        }
        if (levelDropPoint != null) {
            tag.putDouble("LevelDropPointX", levelDropPoint.x);
            tag.putDouble("LevelDropPointY", levelDropPoint.y);
            tag.putDouble("LevelDropPointZ", levelDropPoint.z);
        }
        tag.putBoolean("AppliedSlowness", appliedSlowness);
        tag.putString("LastKnownState", lastKnownState.name());
        tag.putBoolean("IsForcedReturn", isForcedReturn);
        tag.putDouble("OrbitAngle", orbitAngle);
    }
}
