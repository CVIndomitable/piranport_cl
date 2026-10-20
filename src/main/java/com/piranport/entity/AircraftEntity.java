package com.piranport.entity;

import com.piranport.PiranPort;
import com.piranport.aviation.AircraftClass;
import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.AircraftDefinitionService;
import com.piranport.aviation.AircraftIndex;
import com.piranport.aviation.AircraftStatsService;
import com.piranport.aviation.AircraftVisualIds;
import com.piranport.aviation.ReconManager;
import com.piranport.aviation.ResolvedAircraftStats;
import com.piranport.aviation.combat.AircraftAttackContext;
import com.piranport.aviation.combat.DiveBombState;
import com.piranport.aviation.combat.LevelBombState;
import com.piranport.aviation.combat.TorpedoRunState;
import com.piranport.item.ExperienceShellItem;
import com.piranport.component.AircraftAttackMode;
import com.piranport.component.AircraftState;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.network.ReconStatePayload;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import com.piranport.platform.ClientHooks;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * 飞机实体 — 服务端权威控制，客户端插值渲染。
 *
 * <p>本类只持有状态与访问器；飞行主循环在 {@link AircraftFlight}，攻击战术在
 * {@code aviation.combat.strategy}。静态数值来自数据包定义，创建时解析成
 * {@link ResolvedAircraftStats} 快照，飞行中热重载不会改变已起飞的飞机。
 */
public class AircraftEntity extends Entity implements AircraftAttackContext {

    public enum FlightState {
        LAUNCHING, CRUISING, ATTACKING, RETURNING, REMOVED,
        RECON_ACTIVE
    }

    private static final java.util.Map<FlightState, java.util.Set<FlightState>> LEGAL_TRANSITIONS =
            java.util.Map.of(
                    FlightState.LAUNCHING, java.util.Set.of(
                            FlightState.CRUISING, FlightState.RECON_ACTIVE,
                            FlightState.RETURNING, FlightState.REMOVED),
                    FlightState.CRUISING, java.util.Set.of(
                            FlightState.ATTACKING, FlightState.RETURNING, FlightState.REMOVED),
                    FlightState.ATTACKING, java.util.Set.of(
                            FlightState.CRUISING, FlightState.RETURNING, FlightState.REMOVED),
                    FlightState.RECON_ACTIVE, java.util.Set.of(
                            FlightState.RETURNING, FlightState.REMOVED),
                    FlightState.RETURNING, java.util.Set.of(FlightState.REMOVED),
                    FlightState.REMOVED, java.util.Set.of());

    /** 稳定状态 ID；同步与存档都只使用它，不再写 enum ordinal。 */
    private static final EntityDataAccessor<String> STATE_ID_DATA =
            SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.STRING);
    /** 稳定定义 ID（= 飞机物品注册名）。 */
    private static final EntityDataAccessor<String> AIRCRAFT_DEFINITION_DATA =
            SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.STRING);
    /** 视觉 ID 同步给客户端，客户端不需要自己加载服务端数据包即可正确渲染。 */
    private static final EntityDataAccessor<String> VISUAL_ID_DATA =
            SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_ID =
            SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> WEAPON_SLOT_INDEX =
            SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> TORPEDO_AIM_X =
            SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TORPEDO_AIM_Y =
            SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TORPEDO_AIM_Z =
            SynchedEntityData.defineId(AircraftEntity.class, EntityDataSerializers.FLOAT);

    // Saved to NBT
    @Nullable private UUID ownerUUID;
    private int weaponSlotIndex = -1;
    private int coreInventorySlot = 0;
    private ResourceLocation aircraftDefinitionId =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "missing");
    @Nullable private volatile AircraftDefinition cachedDefinition;
    private volatile boolean cachedDefinitionIsPlaceholder = false;
    @Nullable private ResolvedAircraftStats resolvedStats;
    AircraftAttackMode attackMode = AircraftAttackMode.FOCUS;

    int remainingAmmo = 0;
    int ammoCapacity = 0;
    /**
     * 本次挂载是否为核航弹（副本/08）。只对白名单水平轰炸机可能为 true；
     * 投弹时据此把普通航弹的威力与爆炸倍率整体放大，命中阶段不再二次乘系数。
     */
    boolean nuclearPayload = false;
    private int currentFuel = 0;
    private int fuelCapacity = 1;
    float panelSpeed = 1.0f;
    float panelDamageBase = 0.0f;

    // 运行时（部分需跨区块卸载保存）
    int airtimeTicks = 0;
    int stuckTicks = 0;
    Vec3 stuckCheckPos = Vec3.ZERO;
    double orbitAngle = 0;
    int stateTicks = 0;
    int rocketRunTicks = 0;
    int attackCooldown = 0;
    private int flightAttackCooldown = 1;
    private int flightMaxHealth = 0;
    int aircraftHealth = 20;
    boolean hasFired = false;

    // 各攻击 profile 的每架次状态
    final DiveBombState diveBombState = new DiveBombState();
    final TorpedoRunState torpedoRunState = new TorpedoRunState();
    final LevelBombState levelBombState = new LevelBombState();

    int autoSeekCooldown = 20;
    boolean autoSeekDone = false;
    boolean hasEverHadFireControl = false;

    FlightState lastKnownState = FlightState.LAUNCHING;
    int lastForcedChunkX = Integer.MIN_VALUE;
    int lastForcedChunkZ = Integer.MIN_VALUE;

    final java.util.Set<Long> reconForcedChunks = new java.util.HashSet<>();
    final java.util.Set<Long> reconPendingSend = new java.util.HashSet<>();

    @Nullable AircraftEntity cachedReconAircraft;
    int reconCacheTick = 0;
    @Nullable int[] cachedMapSlots;
    int mapSlotsRefreshTick = -ModEquipmentConfig.RECON_MAP_REFRESH_TICKS.get();

    private int clientLerpSteps;
    private double clientLerpX, clientLerpY, clientLerpZ;
    private float clientLerpYRot, clientLerpXRot;

    private ItemStack originalStack = ItemStack.EMPTY;
    boolean isForcedReturn = false;
    private final long spawnTimeMs = System.currentTimeMillis();
    @Nullable private String lastReturnResult = null;
    private boolean indexRegistered = false;
    private int radarGlowTicks = 0;
    /** 自卫机枪的独立射击冷却；与主武器 attackCooldown 无关。 */
    int selfDefenseCooldown = 0;
    @Nullable Vec3 lastHorizontalDir = null;

    boolean autonomous = false;
    Vec3 homePosition = Vec3.ZERO;
    @Nullable LivingEntity autonomousTarget = null;
    @Nullable private AbstractDeepOceanEntity aircraftOwner;

    public AircraftEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** 玩家所属飞机的工厂方法 — 仅在服务端调用。 */
    public static AircraftEntity create(Level level, Player owner, int weaponSlotIndex,
                                        ItemStack aircraftStack, AircraftAttackMode attackMode,
                                        int coreInventorySlot) {
        AircraftEntity entity = new AircraftEntity(ModEntityTypes.AIRCRAFT_ENTITY.get(), level);
        entity.ownerUUID = owner.getUUID();
        entity.weaponSlotIndex = weaponSlotIndex;
        entity.coreInventorySlot = coreInventorySlot;
        entity.attackMode = attackMode;
        entity.initFromStack(aircraftStack, owner, false);
        entity.entityData.set(OWNER_ID, Optional.of(owner.getUUID()));
        entity.entityData.set(WEAPON_SLOT_INDEX, weaponSlotIndex);

        Vec3 look = owner.getLookAngle();
        double spawnDistance = 0.45;
        entity.setPos(owner.getX() + look.x * spawnDistance, owner.getEyeY() - 0.15,
                owner.getZ() + look.z * spawnDistance);
        entity.orbitAngle = Math.atan2(look.z, look.x);
        entity.stuckCheckPos = entity.position();
        return entity;
    }

    /** 自主飞机（无玩家所有者），由深海航母或调试命令创建。 */
    public static AircraftEntity createAutonomous(Level level, Vec3 spawnPos, ItemStack aircraftStack,
                                                  @Nullable LivingEntity target,
                                                  @Nullable AbstractDeepOceanEntity aircraftOwner) {
        AircraftEntity entity = new AircraftEntity(ModEntityTypes.AIRCRAFT_ENTITY.get(), level);
        entity.autonomous = true;
        entity.homePosition = spawnPos;
        entity.autonomousTarget = target;
        entity.aircraftOwner = aircraftOwner;
        entity.initFromStack(aircraftStack, null, true);
        entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        entity.orbitAngle = level.random.nextDouble() * Math.PI * 2;
        entity.stuckCheckPos = entity.position();
        return entity;
    }

    /**
     * 玩家与自主飞机共用的初始化：定义 → 快照 → 同步字段。
     * 自主飞机默认满油，玩家飞机读取物品上的运行时状态。
     */
    private void initFromStack(ItemStack stack, @Nullable Player owner, boolean fullFuel) {
        AircraftDefinition definition = AircraftDefinitionService.require(stack);
        AircraftState state = AircraftDefinitionService.stateOf(stack);
        ResolvedAircraftStats resolved = AircraftStatsService.resolve(definition);
        this.aircraftDefinitionId = definition.id();
        this.cachedDefinition = definition;
        this.resolvedStats = resolved;
        this.panelDamageBase = resolved.damage();
        this.panelSpeed = ExperienceShellItem.applyAircraftPanelSpeedBonus(owner, resolved.speed());
        this.ammoCapacity = resolved.ammoCapacity();
        this.remainingAmmo = resolved.ammoCapacity();
        this.nuclearPayload = state.nuclearPayload();
        this.fuelCapacity = resolved.fuelCapacity();
        this.currentFuel = fullFuel
                ? resolved.fuelCapacity()
                : Math.min(state.currentFuel(), resolved.fuelCapacity());
        this.aircraftHealth = resolved.health();
        this.flightMaxHealth = resolved.health();
        this.flightAttackCooldown = resolved.cooldown();
        this.originalStack = stack.copy();
        entityData.set(AIRCRAFT_DEFINITION_DATA, definition.id().toString());
        entityData.set(VISUAL_ID_DATA, definition.visualId());
        entityData.set(STATE_ID_DATA, stateId(FlightState.LAUNCHING));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE_ID_DATA, stateId(FlightState.LAUNCHING));
        builder.define(AIRCRAFT_DEFINITION_DATA,
                ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "missing").toString());
        builder.define(VISUAL_ID_DATA, AircraftVisualIds.F4F);
        builder.define(OWNER_ID, Optional.empty());
        builder.define(WEAPON_SLOT_INDEX, -1);
        builder.define(TORPEDO_AIM_X, 0.0f);
        builder.define(TORPEDO_AIM_Y, 0.0f);
        builder.define(TORPEDO_AIM_Z, 0.0f);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.clientLerpX = x;
        this.clientLerpY = y;
        this.clientLerpZ = z;
        this.clientLerpYRot = yRot;
        this.clientLerpXRot = xRot;
        this.clientLerpSteps = steps;
    }

    @Override public double lerpTargetX() { return clientLerpSteps > 0 ? clientLerpX : getX(); }
    @Override public double lerpTargetY() { return clientLerpSteps > 0 ? clientLerpY : getY(); }
    @Override public double lerpTargetZ() { return clientLerpSteps > 0 ? clientLerpZ : getZ(); }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide() && radarGlowTicks > 0 && --radarGlowTicks == 0) {
            setGlowingTag(false);
        }

        if (level().isClientSide()) {
            if (clientLerpSteps > 0) {
                double factor = 1.0 / (double) clientLerpSteps;
                setPos(getX() + (clientLerpX - getX()) * factor,
                        getY() + (clientLerpY - getY()) * factor,
                        getZ() + (clientLerpZ - getZ()) * factor);
                float yDiff = clientLerpYRot - getYRot();
                while (yDiff > 180f) yDiff -= 360f;
                while (yDiff < -180f) yDiff += 360f;
                setYRot(getYRot() + yDiff * (float) factor);
                setXRot(getXRot() + (clientLerpXRot - getXRot()) * (float) factor);
                clientLerpSteps--;
            }
            if (getFlightState() == FlightState.CRUISING && tickCount % 8 == 0) {
                level().addParticle(ParticleTypes.CLOUD, getX(), getY(), getZ(), 0, 0.02, 0);
            }
            return;
        }

        boolean perfEnabled = com.piranport.debug.PiranPortDebug.isServerEnabled();
        long start = perfEnabled ? System.nanoTime() : 0L;

        if (!indexRegistered && ownerUUID != null) {
            AircraftIndex.add(ownerUUID, this);
            indexRegistered = true;
        }

        if (getFlightState() == FlightState.REMOVED) {
            discard();
            return;
        }

        Player owner = getOwner();
        if (owner == null && !autonomous) {
            if (ownerUUID != null && getFlightState() == FlightState.RECON_ACTIVE) {
                ReconManager.endRecon(ownerUUID);
                releaseAllForcedChunks();
            }
            discard();
            return;
        }

        AircraftFlight.tickServer(this, owner);

        if (perfEnabled) {
            long nanos = System.nanoTime() - start;
            if (nanos > 5_000_000L) {
                com.piranport.debug.PiranPortDebug.perf("AircraftTick", nanos,
                        "entityId=" + getId() + " state=" + getFlightState().name()
                                + " class=" + getAircraftClass().getSerializedName());
            }
        }
    }

    // ===== 状态机 =====

    public void setState(FlightState newState) {
        FlightState from = getFlightState();
        if (from == newState) return;
        java.util.Set<FlightState> allowed = LEGAL_TRANSITIONS.getOrDefault(from, java.util.Set.of());
        if (!allowed.contains(newState)) {
            throw new IllegalStateException(
                    "illegal aircraft flight state transition: " + from + " -> " + newState
                            + " (entityId=" + getId() + ", def=" + aircraftDefinitionId + ")");
        }
        if (newState == FlightState.ATTACKING) {
            hasFired = false;
            attackCooldown = 0;
            diveBombState.reset();
            torpedoRunState.reset();
            levelBombState.reset();
            rocketRunTicks = 0;
            setTorpedoAimPoint(null);
        }
        entityData.set(STATE_ID_DATA, stateId(newState));
        stateTicks = 0;
    }

    void handleStateTransition(FlightState from, FlightState to, Player owner) {
        if (to == FlightState.RECON_ACTIVE) {
            ReconManager.startRecon(owner.getUUID(), getUUID());
            if (owner instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new ReconStatePayload(true, getId()));
            }
        } else if (from == FlightState.RECON_ACTIVE) {
            if (to == FlightState.RETURNING) {
                beginReconReturn(owner);
            } else {
                cleanupReconState(owner);
            }
        }
    }

    /**
     * 退出侦察转入返航：结束侦察操控，但保留一小圈强加载直到回家。
     *
     * <p>WHY 不直接 {@link #cleanupReconState}：侦察机可能已在实体 tick 范围之外，
     * 立刻释放区块会让返航永久冻结。返航票据由 {@code AircraftAswRecon.tickReturnLoading}
     * 随飞机移动，最终由 {@link #remove} 统一释放。
     */
    void beginReconReturn(Player owner) {
        ReconManager.endRecon(owner.getUUID());
        AircraftAswRecon.beginReconReturnLoading(this);
        if (owner instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new ReconStatePayload(false, 0));
        }
    }

    public void cleanupReconState(Player owner) {
        ReconManager.endRecon(owner.getUUID());
        releaseAllForcedChunks();
        if (owner instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new ReconStatePayload(false, 0));
        }
    }

    public void startReturning(String reason) {
        FlightState current = getFlightState();
        if (current == FlightState.RETURNING || current == FlightState.REMOVED) return;
        if (com.piranport.debug.PiranPortDebug.shouldEmit()) {
            String pos = String.format("[%d,%d,%d]", blockPosition().getX(),
                    blockPosition().getY(), blockPosition().getZ());
            com.piranport.debug.PiranPortDebug.aircraftReturnTriggered(
                    getId(), reason, current, pos);
            com.piranport.debug.PiranPortDebug.event(
                    "Aircraft RETURNING | class={} entityId={} reason={}",
                    getAircraftClass().getSerializedName(), getId(), reason);
        }
        if (getOwner() instanceof ServerPlayer owner
                && com.piranport.dungeon.weather.WeatherRuntime.blocksLanding(owner)) {
            abandonOnNightLanding(owner);
            return;
        }
        setState(FlightState.RETURNING);
    }

    private void abandonOnNightLanding(ServerPlayer owner) {
        if (getFlightState() == FlightState.RECON_ACTIVE) cleanupReconState(owner);
        owner.displayClientMessage(Component.translatable("message.piranport.weather_no_landing"), true);
        setState(FlightState.REMOVED);
        discard();
    }

    public void recallAndRemove() {
        AircraftResupply.recallAndRemove(this);
    }

    void releaseAllForcedChunks() {
        AircraftAswRecon.releaseAllForcedChunks(this);
    }

    // ===== 交互 =====

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide() || ownerUUID == null || !ownerUUID.equals(player.getUUID())) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.FUEL.get())) {
            if (currentFuel < fuelCapacity) {
                held.shrink(1);
                currentFuel = fuelCapacity;
                player.displayClientMessage(
                        Component.translatable("message.piranport.aircraft_refueled", getDisplayName()),
                        true);
                return InteractionResult.sidedSuccess(false);
            }
        }

        ResourceLocation payloadItem = getPayloadItemId();
        if (!held.isEmpty() && payloadItem != null) {
            ResourceLocation heldId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getKey(held.getItem());
            // 核航弹是普通航弹的替代装填项（副本/08），只有白名单机型能手动补核航弹。
            boolean nuclear = held.is(ModItems.NUCLEAR_AERIAL_BOMB.get())
                    && getResolvedDefinition().canCarryNuclearBomb();
            if ((nuclear || payloadItem.equals(heldId)) && remainingAmmo < ammoCapacity) {
                int needed = ammoCapacity - remainingAmmo;
                int toLoad = Math.min(needed, held.getCount());
                held.shrink(toLoad);
                remainingAmmo += toLoad;
                // 补普通航弹时只有在弹舱为空的情况下才清掉核弹标记，避免混装时误降级。
                if (nuclear) {
                    nuclearPayload = true;
                } else if (remainingAmmo == toLoad) {
                    nuclearPayload = false;
                }
                hasFired = false;
                player.displayClientMessage(
                        Component.translatable("message.piranport.aircraft_resupplied", getDisplayName()),
                        true);
                return InteractionResult.sidedSuccess(false);
            }
        }

        if (held.isEmpty()) {
            startReturning("player_interact");
            return InteractionResult.sidedSuccess(false);
        }
        return InteractionResult.PASS;
    }

    // ===== 受伤 / 击落 =====

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide()) return false;
        FlightState state = getFlightState();
        if (state == FlightState.REMOVED) return false;

        Entity attacker = source.getEntity();
        if (ownerUUID != null) {
            if (attacker instanceof Player player && ownerUUID.equals(player.getUUID())) return false;
            if (attacker instanceof AircraftEntity aircraft
                    && ownerUUID.equals(aircraft.getOwnerUUID())) return false;
        }
        if (autonomous && attacker instanceof AircraftEntity aircraft
                && aircraft.isAutonomous() && aircraft != this) {
            return false;
        }

        aircraftHealth = Math.max(0, Math.min(getAircraftMaxHealth(), aircraftHealth)
                - (int) Math.ceil(Math.max(0.0F, amount)));

        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.IRON_GOLEM_HURT, SoundSource.HOSTILE,
                0.4f, 1.4f + random.nextFloat() * 0.2f);

        if (aircraftHealth <= 0) {
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE,
                    1.0f, 0.9f + random.nextFloat() * 0.2f);
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(),
                        1, 0.3, 0.3, 0.3, 0.0);
            }
            Player owner = getOwner();
            if (state == FlightState.RECON_ACTIVE && ownerUUID != null) {
                if (owner != null) cleanupReconState(owner);
                else {
                    ReconManager.endRecon(ownerUUID);
                    releaseAllForcedChunks();
                }
            }
            if (owner != null) {
                Component aircraftName = AircraftResupply.buildReturnStack(this).getHoverName();
                if (attacker != null) {
                    owner.displayClientMessage(Component.translatable(
                            "message.piranport.aircraft_shot_down_by",
                            aircraftName, attacker.getDisplayName()), true);
                } else {
                    owner.displayClientMessage(Component.translatable(
                            "message.piranport.aircraft_shot_down", aircraftName), true);
                }
            }
            com.piranport.debug.PiranPortDebug.event(
                    "Aircraft KILLED | class={} entityId={} attacker={}",
                    getAircraftClass().getSerializedName(), getId(),
                    attacker != null ? attacker.getType().toShortString() : "unknown");
            ItemStack dropStack = AircraftResupply.buildReturnStack(this);
            if (!dropStack.isEmpty()) {
                AircraftDropEntity drop = new AircraftDropEntity(level(), getX(), getY(), getZ(), dropStack);
                level().addFreshEntity(drop);
            }
            discard();
        }
        return true;
    }

    // ===== 定义 / 能力查询 =====

    /** 返回实体创建时解析的稳定飞机定义 ID。 */
    public ResourceLocation getAircraftDefinitionId() {
        String synced = entityData.get(AIRCRAFT_DEFINITION_DATA);
        if (synced == null || synced.isBlank()) return aircraftDefinitionId;
        ResourceLocation parsed = ResourceLocation.tryParse(synced);
        return parsed == null ? aircraftDefinitionId : parsed;
    }

    /**
     * 解析当前定义并缓存。客户端也会通过自己的资源包加载定义；若两端定义不一致，
     * 渲染仍使用同步过来的 {@code visual_id}，不依赖这里的定义。
     */
    public AircraftDefinition getResolvedDefinition() {
        ResourceLocation id = getAircraftDefinitionId();
        AircraftDefinition cached = cachedDefinition;
        if (cached != null && cached.id().equals(id) && !cachedDefinitionIsPlaceholder) {
            return cached;
        }
        boolean wasPlaceholder = cachedDefinitionIsPlaceholder;
        AircraftDefinition resolved = AircraftDefinitionService.find(id);
        boolean placeholder = false;
        if (resolved != null && wasPlaceholder) {
            // 客户端先于定义同步渲染过占位定义；拿到真定义后重建统计快照。
            resolvedStats = null;
        }
        if (resolved == null) {
            PiranPort.LOGGER.error("Missing aircraft definition {} for entity {}; using placeholder",
                    id, getId());
            resolved = new AircraftDefinition(id, AircraftClass.FIGHTER,
                    AircraftDefinition.AttackProfile.NONE, null, AircraftVisualIds.F4F,
                    1, 0, 0.0F, 1.0F, 0, 1, 1, null);
            placeholder = true;
        }
        cachedDefinition = resolved;
        cachedDefinitionIsPlaceholder = placeholder;
        return resolved;
    }

    private ResolvedAircraftStats resolveStats() {
        if (resolvedStats == null) {
            resolvedStats = AircraftStatsService.resolve(getResolvedDefinition());
        }
        return resolvedStats;
    }

    public AircraftClass getAircraftClass() {
        return getResolvedDefinition().aircraftClass();
    }

    /** 侦察机返回 null（不声明攻击配置）。 */
    @Nullable
    public AircraftDefinition.AttackProfile getAttackProfile() {
        return getResolvedDefinition().attackProfile();
    }

    public boolean requiresPayload() {
        return getResolvedDefinition().requiresPayload();
    }

    public boolean usesBulletAttack() {
        return getResolvedDefinition().usesBulletAttack();
    }

    public boolean isPayloadAttacker() {
        return getResolvedDefinition().isPayloadAttacker();
    }

    public boolean hasSonar() {
        return getResolvedDefinition().hasSonar();
    }

    public boolean isReconAircraft() {
        return getResolvedDefinition().isRecon();
    }

    @Nullable
    public ResourceLocation getPayloadItemId() {
        return getResolvedDefinition().payloadItem();
    }

    public String getVisualId() {
        String synced = entityData.get(VISUAL_ID_DATA);
        return synced == null || synced.isBlank()
                ? getResolvedDefinition().visualId() : synced;
    }

    public int getAircraftHealth() {
        return Math.max(0, aircraftHealth);
    }

    public int getAircraftMaxHealth() {
        return Math.max(1, flightMaxHealth > 0 ? flightMaxHealth : aircraftHealth);
    }

    @Override
    public AircraftDefinition definition() {
        return getResolvedDefinition();
    }

    @Override
    public ResolvedAircraftStats stats() {
        return resolveStats();
    }

    @Override
    public float panelDamage() {
        Player owner = getOwner();
        return owner != null
                ? ExperienceShellItem.applyAircraftPanelDamageBonus(owner, panelDamageBase)
                : panelDamageBase;
    }

    @Override
    public float panelSpeed() {
        return panelSpeed;
    }

    @Override
    public double phaseSpeedCoefficient() {
        String property = switch (getFlightState()) {
            case LAUNCHING -> "launch_speed_coefficient";
            case CRUISING -> attackMode == AircraftAttackMode.FOLLOW
                    ? "follow_speed_coefficient" : "cruise_speed_coefficient";
            case ATTACKING -> "attack_speed_coefficient";
            case RETURNING -> "return_speed_coefficient";
            case RECON_ACTIVE -> "recon_speed_coefficient";
            case REMOVED -> null;
        };
        if (property == null) return 1.0;
        double multiplier = com.piranport.terminal.TerminalParameters.getDouble(
                "aircraft." + aircraftDefinitionId + "." + property, 1.0);
        return Double.isFinite(multiplier) ? Math.max(0.05, Math.min(5.0, multiplier)) : 1.0;
    }

    // ===== 上下文访问器 =====

    @Override public Entity self() { return this; }
    @Override public Vec3 eyePosition() { return getEyePosition(); }
    @Override @Nullable public ServerLevel serverLevel() {
        return level() instanceof ServerLevel serverLevel ? serverLevel : null;
    }
    @Override @Nullable public Player owner() { return getOwner(); }
    @Override @Nullable public UUID ownerUuid() { return ownerUUID; }
    @Override public double distanceToEntity(Entity entity) { return super.distanceTo(entity); }
    @Override public double distanceToEntitySqr(Entity entity) { return super.distanceToSqr(entity); }
    @Override public AABB boundingBox() { return getBoundingBox(); }
    @Override public Vec3 deltaMovement() { return getDeltaMovement(); }
    @Override public float yRot() { return getYRot(); }
    @Override public float xRot() { return getXRot(); }
    @Override public int entityId() { return getId(); }
    @Override public Component displayName() { return getDisplayName(); }
    public int ammoCapacity() { return ammoCapacity; }
    public int remainingAmmo() { return remainingAmmo; }
    public void setRemainingAmmo(int ammo) { remainingAmmo = Math.max(0, ammo); }
    public int attackCooldown() { return attackCooldown; }
    public void setAttackCooldown(int ticks) { attackCooldown = Math.max(0, ticks); }
    public int attackCooldownDuration() { return Math.max(1, flightAttackCooldown); }
    public int stateTicks() { return stateTicks; }
    @Override public int rocketRunTicks() { return rocketRunTicks; }
    @Override public void setRocketRunTicks(int ticks) { rocketRunTicks = Math.max(0, ticks); }
    public void resumeCruising() {
        if (getFlightState() == FlightState.ATTACKING) setState(FlightState.CRUISING);
    }
    public boolean hasFired() { return hasFired; }
    public void setHasFired(boolean fired) { hasFired = fired; }
    public AircraftAttackMode attackMode() { return attackMode; }
    public boolean hasEverHadFireControl() { return hasEverHadFireControl; }
    public boolean autoSeekDone() { return autoSeekDone; }
    public void setAutoSeekDone(boolean done) { autoSeekDone = done; }
    public int autoSeekCooldown() { return autoSeekCooldown; }
    public void setAutoSeekCooldown(int ticks) { autoSeekCooldown = Math.max(0, ticks); }
    @Nullable public LivingEntity autonomousTarget() { return autonomousTarget; }

    @Nullable
    public Player getOwner() {
        if (ownerUUID == null) return null;
        if (level() instanceof ServerLevel serverLevel) {
            return serverLevel.getServer().getPlayerList().getPlayer(ownerUUID);
        }
        return null;
    }

    @Nullable public UUID getOwnerUUID() { return ownerUUID; }
    public boolean isAutonomous() { return autonomous; }
    public boolean isOwnedByPlayer(Player player) {
        Optional<UUID> id = entityData.get(OWNER_ID);
        return id.isPresent() && id.get().equals(player.getUUID());
    }

    // ===== 鱼雷预瞄点 =====

    public void setTorpedoAimPoint(@Nullable Vec3 point) {
        torpedoRunState.aimPoint = point;
        if (point == null) {
            entityData.set(TORPEDO_AIM_X, 0.0f);
            entityData.set(TORPEDO_AIM_Y, 0.0f);
            entityData.set(TORPEDO_AIM_Z, 0.0f);
        } else {
            entityData.set(TORPEDO_AIM_X, (float) point.x);
            entityData.set(TORPEDO_AIM_Y, (float) point.y);
            entityData.set(TORPEDO_AIM_Z, (float) point.z);
        }
    }

    @Nullable
    public Vec3 getTorpedoAimPoint() {
        float x = entityData.get(TORPEDO_AIM_X);
        float y = entityData.get(TORPEDO_AIM_Y);
        float z = entityData.get(TORPEDO_AIM_Z);
        if (x == 0.0f && y == 0.0f && z == 0.0f) return null;
        return new Vec3(x, y, z);
    }

    @Override public Vec3 torpedoAimPoint() { return getTorpedoAimPoint(); }
    @Override public boolean nuclearPayload() { return nuclearPayload; }
    @Override public DiveBombState diveBombState() { return diveBombState; }
    @Override public TorpedoRunState torpedoRunState() { return torpedoRunState; }
    @Override public LevelBombState levelBombState() { return levelBombState; }

    // ===== 状态 ID =====

    private static final FlightState[] FLIGHT_STATE_VALUES = FlightState.values();

    static String stateId(FlightState state) {
        return state.name().toLowerCase(java.util.Locale.ROOT);
    }

    static FlightState stateById(String id) {
        if (id == null || id.isBlank()) return null;
        String normalized = id.trim().toLowerCase(java.util.Locale.ROOT);
        for (FlightState candidate : FLIGHT_STATE_VALUES) {
            if (stateId(candidate).equals(normalized)) return candidate;
        }
        return null;
    }

    public FlightState getFlightState() {
        FlightState state = stateById(entityData.get(STATE_ID_DATA));
        return state != null ? state : FlightState.LAUNCHING;
    }

    // ===== 杂项 =====

    public void markRadarGlow(int ticks) {
        radarGlowTicks = ticks;
        setGlowingTag(true);
    }

    @Override
    public boolean isCurrentlyGlowing() {
        if (super.isCurrentlyGlowing()) return true;
        if (level().isClientSide()) return ClientHooks.shouldAircraftGlow(this);
        return false;
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable("entity.piranport.aircraft."
                + getAircraftClass().getSerializedName());
    }

    @Override
    public int getTeamColor() {
        if (level().isClientSide()) {
            if (super.isCurrentlyGlowing()) return super.getTeamColor();
            return ClientHooks.getAircraftGlowColor(this, super.getTeamColor());
        }
        return super.getTeamColor();
    }

    @Override public boolean isPickable() { return true; }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide()) {
            releaseAllForcedChunks();
            if (ownerUUID != null) ReconManager.endRecon(ownerUUID);
        }
        super.remove(reason);
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide() && ownerUUID != null) {
            AircraftIndex.add(ownerUUID, this);
            indexRegistered = true;
        }
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        if (!level().isClientSide()) {
            if (ownerUUID != null) {
                AircraftIndex.remove(ownerUUID, this);
                indexRegistered = false;
            }
            if (aircraftOwner != null && !aircraftOwner.isRemoved()) {
                aircraftOwner.onAircraftReturned();
            }
        }
    }

    // ===== 持久化 =====

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) ownerUUID = tag.getUUID("OwnerUUID");
        weaponSlotIndex = tag.contains("WeaponSlot") ? tag.getInt("WeaponSlot") : -1;
        coreInventorySlot = tag.contains("CoreSlot") ? tag.getInt("CoreSlot") : 0;
        entityData.set(WEAPON_SLOT_INDEX, weaponSlotIndex);

        ResourceLocation id = ResourceLocation.tryParse(tag.getString("AircraftDefinitionId"));
        if (id == null) {
            PiranPort.LOGGER.error("Aircraft entity {} has no AircraftDefinitionId; it will not fly",
                    getId());
            id = ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "missing");
        }
        aircraftDefinitionId = id;
        entityData.set(AIRCRAFT_DEFINITION_DATA, id.toString());
        String visual = tag.getString("VisualId");
        entityData.set(VISUAL_ID_DATA, visual.isBlank() ? AircraftVisualIds.F4F : visual);

        AircraftDefinition definition = AircraftDefinitionService.find(id);
        if (definition != null) {
            cachedDefinition = definition;
            cachedDefinitionIsPlaceholder = false;
            resolvedStats = AircraftStatsService.resolve(definition);
        }

        try { attackMode = AircraftAttackMode.valueOf(tag.getString("AttackMode")); }
        catch (IllegalArgumentException ignored) { attackMode = AircraftAttackMode.FOCUS; }

        currentFuel = tag.getInt("CurrentFuel");
        remainingAmmo = tag.getInt("RemainingAmmo");
        nuclearPayload = tag.getBoolean("NuclearPayload");
        ammoCapacity = tag.getInt("AmmoCapacity");
        fuelCapacity = Math.max(1, tag.getInt("FuelCapacity"));
        panelSpeed = tag.getFloat("PanelSpeed");
        panelDamageBase = tag.getFloat("PanelDamage");
        aircraftHealth = tag.getInt("AircraftHealth");
        flightMaxHealth = Math.max(1, tag.getInt("FlightMaxHealth"));
        flightAttackCooldown = Math.max(1, tag.getInt("FlightAttackCooldown"));
        // 旧/损坏存档缺字段时用当前定义补齐；正常路径全部来自飞行快照。
        if (resolvedStats != null) {
            if (fuelCapacity <= 1 && tag.getInt("FuelCapacity") <= 0) fuelCapacity = resolvedStats.fuelCapacity();
            if (ammoCapacity <= 0 && tag.getInt("AmmoCapacity") <= 0) ammoCapacity = resolvedStats.ammoCapacity();
            if (panelSpeed <= 0.0f) panelSpeed = resolvedStats.speed();
            if (panelDamageBase <= 0.0f) panelDamageBase = resolvedStats.damage();
            if (aircraftHealth <= 0 && !tag.contains("AircraftHealth")) aircraftHealth = resolvedStats.health();
            if (!tag.contains("FlightMaxHealth")) flightMaxHealth = resolvedStats.health();
            if (!tag.contains("FlightAttackCooldown")) flightAttackCooldown = resolvedStats.cooldown();
        }
        if (panelSpeed <= 0.0f) panelSpeed = 1.0f;
        aircraftHealth = Math.min(Math.max(0, aircraftHealth), flightMaxHealth);
        if (remainingAmmo > ammoCapacity) remainingAmmo = ammoCapacity;
        if (!tag.contains("RemainingAmmo")) remainingAmmo = ammoCapacity;

        lastForcedChunkX = tag.contains("LastForcedChunkX")
                ? tag.getInt("LastForcedChunkX") : Integer.MIN_VALUE;
        lastForcedChunkZ = tag.contains("LastForcedChunkZ")
                ? tag.getInt("LastForcedChunkZ") : Integer.MIN_VALUE;
        if (tag.contains("ReconForcedChunks") && level() instanceof ServerLevel serverLevel) {
            for (long key : tag.getLongArray("ReconForcedChunks")) {
                serverLevel.setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),
                        net.minecraft.world.level.ChunkPos.getZ(key), false);
            }
        }
        reconForcedChunks.clear();

        FlightState state = stateById(tag.getString("FlightStateId"));
        if (state == null) state = FlightState.LAUNCHING;
        entityData.set(STATE_ID_DATA, stateId(state));
        entityData.set(OWNER_ID, ownerUUID == null ? Optional.empty() : Optional.of(ownerUUID));

        if (getFlightState() == FlightState.RECON_ACTIVE && ownerUUID != null
                && level() instanceof ServerLevel serverLevel) {
            ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerUUID);
            if (owner == null) setState(FlightState.RETURNING);
        }

        airtimeTicks = tag.getInt("AirtimeTicks");
        hasFired = tag.getBoolean("HasFired");
        stateTicks = tag.getInt("StateTicks");
        attackCooldown = tag.getInt("AttackCooldown");
        autoSeekDone = tag.getBoolean("AutoSeekDone");
        hasEverHadFireControl = tag.getBoolean("HasEverHadFireControl");
        autoSeekCooldown = tag.getInt("AutoSeekCooldown");
        diveBombState.committed = tag.getBoolean("DiveCommitted");
        if (tag.contains("DiveTargetX")) {
            diveBombState.target = new Vec3(tag.getDouble("DiveTargetX"),
                    tag.getDouble("DiveTargetY"), tag.getDouble("DiveTargetZ"));
        }
        if (tag.contains("TorpedoRunStartX")) {
            torpedoRunState.runStart = new Vec3(tag.getDouble("TorpedoRunStartX"),
                    tag.getDouble("TorpedoRunStartY"), tag.getDouble("TorpedoRunStartZ"));
        }
        torpedoRunState.runTicks = tag.getInt("TorpedoRunTicks");
        if (tag.contains("TorpedoAimX")) {
            torpedoRunState.aimPoint = new Vec3(tag.getDouble("TorpedoAimX"),
                    tag.getDouble("TorpedoAimY"), tag.getDouble("TorpedoAimZ"));
            setTorpedoAimPoint(torpedoRunState.aimPoint);
        }
        levelBombState.dropped = tag.getBoolean("LevelBombDropped");
        levelBombState.runTicks = tag.getInt("LevelRunTicks");
        levelBombState.abortTicks = tag.getInt("LevelAbortTicks");
        levelBombState.rearApproachDone = tag.getBoolean("LevelRearApproachDone");
        if (tag.contains("LevelRunDirX")) {
            levelBombState.runDirection = new Vec3(tag.getDouble("LevelRunDirX"),
                    tag.getDouble("LevelRunDirY"), tag.getDouble("LevelRunDirZ"));
        }
        if (tag.contains("LevelDropPointX")) {
            levelBombState.dropPoint = new Vec3(tag.getDouble("LevelDropPointX"),
                    tag.getDouble("LevelDropPointY"), tag.getDouble("LevelDropPointZ"));
        }
        if (tag.contains("LastKnownState")) {
            try { lastKnownState = FlightState.valueOf(tag.getString("LastKnownState")); }
            catch (IllegalArgumentException ignored) { lastKnownState = FlightState.LAUNCHING; }
        }
        isForcedReturn = tag.getBoolean("IsForcedReturn");
        if (tag.contains("OrbitAngle")) orbitAngle = tag.getDouble("OrbitAngle");
        if (tag.contains("OriginalStack")) {
            originalStack = ItemStack.parse(level().registryAccess(), tag.getCompound("OriginalStack"))
                    .orElse(ItemStack.EMPTY);
        }
        autonomous = tag.getBoolean("Autonomous");
        if (tag.contains("HomeX")) {
            homePosition = new Vec3(tag.getDouble("HomeX"), tag.getDouble("HomeY"),
                    tag.getDouble("HomeZ"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUUID != null) tag.putUUID("OwnerUUID", ownerUUID);
        tag.putInt("WeaponSlot", weaponSlotIndex);
        tag.putInt("CoreSlot", coreInventorySlot);
        tag.putString("AircraftDefinitionId", getAircraftDefinitionId().toString());
        tag.putString("VisualId", getVisualId());
        tag.putString("AttackMode", attackMode.name());
        tag.putInt("CurrentFuel", currentFuel);
        tag.putInt("RemainingAmmo", remainingAmmo);
        tag.putBoolean("NuclearPayload", nuclearPayload);
        tag.putInt("AmmoCapacity", ammoCapacity);
        tag.putInt("FuelCapacity", fuelCapacity);
        tag.putFloat("PanelSpeed", panelSpeed);
        tag.putFloat("PanelDamage", panelDamageBase);
        tag.putInt("AircraftHealth", aircraftHealth);
        tag.putInt("FlightMaxHealth", flightMaxHealth > 0 ? flightMaxHealth : Math.max(1, aircraftHealth));
        tag.putInt("FlightAttackCooldown", Math.max(1, flightAttackCooldown));
        if (!reconForcedChunks.isEmpty()) {
            long[] chunks = new long[reconForcedChunks.size()];
            int index = 0;
            for (long key : reconForcedChunks) chunks[index++] = key;
            tag.putLongArray("ReconForcedChunks", chunks);
        }
        tag.putInt("LastForcedChunkX", lastForcedChunkX);
        tag.putInt("LastForcedChunkZ", lastForcedChunkZ);
        tag.putString("FlightStateId", stateId(getFlightState()));
        tag.putInt("AirtimeTicks", airtimeTicks);
        tag.putBoolean("HasFired", hasFired);
        tag.putInt("StateTicks", stateTicks);
        tag.putInt("AttackCooldown", attackCooldown);
        tag.putBoolean("AutoSeekDone", autoSeekDone);
        tag.putBoolean("HasEverHadFireControl", hasEverHadFireControl);
        tag.putInt("AutoSeekCooldown", autoSeekCooldown);
        tag.putBoolean("DiveCommitted", diveBombState.committed);
        if (diveBombState.target != null) {
            tag.putDouble("DiveTargetX", diveBombState.target.x);
            tag.putDouble("DiveTargetY", diveBombState.target.y);
            tag.putDouble("DiveTargetZ", diveBombState.target.z);
        }
        if (torpedoRunState.runStart != null) {
            tag.putDouble("TorpedoRunStartX", torpedoRunState.runStart.x);
            tag.putDouble("TorpedoRunStartY", torpedoRunState.runStart.y);
            tag.putDouble("TorpedoRunStartZ", torpedoRunState.runStart.z);
        }
        tag.putInt("TorpedoRunTicks", torpedoRunState.runTicks);
        Vec3 aim = getTorpedoAimPoint();
        if (aim != null) {
            tag.putDouble("TorpedoAimX", aim.x);
            tag.putDouble("TorpedoAimY", aim.y);
            tag.putDouble("TorpedoAimZ", aim.z);
        }
        tag.putBoolean("LevelBombDropped", levelBombState.dropped);
        tag.putInt("LevelRunTicks", levelBombState.runTicks);
        tag.putInt("LevelAbortTicks", levelBombState.abortTicks);
        tag.putBoolean("LevelRearApproachDone", levelBombState.rearApproachDone);
        if (levelBombState.runDirection != null) {
            tag.putDouble("LevelRunDirX", levelBombState.runDirection.x);
            tag.putDouble("LevelRunDirY", levelBombState.runDirection.y);
            tag.putDouble("LevelRunDirZ", levelBombState.runDirection.z);
        }
        if (levelBombState.dropPoint != null) {
            tag.putDouble("LevelDropPointX", levelBombState.dropPoint.x);
            tag.putDouble("LevelDropPointY", levelBombState.dropPoint.y);
            tag.putDouble("LevelDropPointZ", levelBombState.dropPoint.z);
        }
        tag.putString("LastKnownState", lastKnownState.name());
        tag.putBoolean("IsForcedReturn", isForcedReturn);
        tag.putDouble("OrbitAngle", orbitAngle);
        if (originalStack != null && !originalStack.isEmpty()) {
            tag.put("OriginalStack", originalStack.save(level().registryAccess()));
        }
        tag.putBoolean("Autonomous", autonomous);
        if (autonomous) {
            tag.putDouble("HomeX", homePosition.x);
            tag.putDouble("HomeY", homePosition.y);
            tag.putDouble("HomeZ", homePosition.z);
        }
    }

    // ===== 供包内辅助类/上下文使用的访问器 =====

    int getFuelCapacity() { return fuelCapacity; }
    int getCurrentFuel() { return currentFuel; }
    boolean hasFuel() { return currentFuel > 0; }
    void consumeFuel() { if (currentFuel > 0) currentFuel--; }
    void setCurrentFuel(int value) { this.currentFuel = Math.max(0, Math.min(fuelCapacity, value)); }
    void markForcedReturn() { this.isForcedReturn = true; }
    int getWeaponSlotIndex() { return weaponSlotIndex; }
    ItemStack getOriginalStack() { return originalStack; }
    void setLastReturnResult(String result) { this.lastReturnResult = result; }
    boolean isForcedReturn() { return isForcedReturn; }
    long spawnTimeMs() { return spawnTimeMs; }
    int getAirtimeTicks() { return airtimeTicks; }
    void incrementAirtimeTicks() { this.airtimeTicks++; }
}
