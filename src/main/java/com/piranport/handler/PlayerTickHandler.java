package com.piranport.handler;

import com.piranport.item.ShipType;
import com.piranport.item.AutoCIWSItem;

import com.piranport.PiranPort;
import com.piranport.aviation.FireControlManager;
import com.piranport.aviation.ReconManager;
import com.piranport.combat.AASilenceManager;
import com.piranport.combat.AutoModeState;
import com.piranport.combat.TransformationManager;
import com.piranport.component.FuelData;
import com.piranport.config.ModCommonConfig;

import com.piranport.item.KirinHeadbandItem;
import com.piranport.item.FootballArmorItem;
import com.piranport.item.RadarItem;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.ShipCoreCombat;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家 Tick 处理器 — 驱动水上行走及服务端燃料、声呐、自动战斗等。
 *
 * <p><b>线程模型</b>: 水上行走由各逻辑端的玩家实例独立驱动，其余逻辑仅在服务端主线程运行。
 * 服务端缓存使用 ConcurrentHashMap 防御服务器关闭时的竞态条件。
 * <p><b>缓存生命周期</b>:
 *   {@link #lastWeaponLoad} / {@link #lastPlayerPos} / {@link #accumulatedDistance} —
 *   在 {@link #onPlayerLogout(UUID)} 中清理以单向释放内存，
 *   全局清理通过 {@link #clearCaches()} 在 {@link com.piranport.server.ServerGameEvents#onServerStopped} 中调用。
 * <p><b>访问限制</b>: 静态缓存仅供服务端使用，水上行走状态由 {@link WaterWalkingHandler} 隔离到玩家实例。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class PlayerTickHandler {

    // ==================== Tick 间隔常量 ====================
    /** 声呐扫描间隔（tick）*/
    private static final int SONAR_SCAN_INTERVAL = 40;
    /** 减速效果清理间隔（tick）*/
    private static final int SLOWNESS_CLEANUP_INTERVAL = 20;
    /** 自动战斗检查间隔（tick）*/
    private static final int AUTO_COMBAT_INTERVAL = 40;
    /** 负重检查间隔（tick）- 降低频率以提升性能 */
    private static final int INVENTORY_LOAD_CHECK_INTERVAL = 10;
    /** 传送检测距离阈值（格）- 降低以避免误判鞘翅飞行 */
    private static final double TELEPORT_DETECTION_THRESHOLD = 64.0;

    // ==================== 缓存 Maps ====================
    /** 玩家 UUID → 上次背包武器总载重。用于属性重算检测。 */
    private static final Map<UUID, Integer> lastWeaponLoad = new ConcurrentHashMap<>();
    /** 玩家 UUID → 上次 tick 位置。用于计算移动距离（燃料消耗用）。 */
    private static final Map<UUID, Vec3> lastPlayerPos = new ConcurrentHashMap<>();
    /** 玩家 UUID → 累计移动距离。用于按距离驱动的燃料消耗。 */
    private static final Map<UUID, Double> accumulatedDistance = new ConcurrentHashMap<>();
    /** 清理所有缓存（服务器关闭时调用）*/
    public static void clearCaches() {
        lastWeaponLoad.clear();
        lastPlayerPos.clear();
        accumulatedDistance.clear();
    }

    /** 玩家登出时清理该玩家的缓存条目，防止长时间运行内存泄漏 */
    public static void onPlayerLogout(UUID uuid) {
        lastWeaponLoad.remove(uuid);
        lastPlayerPos.remove(uuid);
        accumulatedDistance.remove(uuid);
    }

    /** 定期清理离线玩家的缓存条目，防止服务器崩溃导致的内存泄漏 */
    public static void cleanupOfflinePlayers(net.minecraft.server.MinecraftServer server) {
        java.util.Set<UUID> onlineUuids = server.getPlayerList().getPlayers().stream()
                .map(net.minecraft.world.entity.Entity::getUUID)
                .collect(java.util.stream.Collectors.toSet());

        lastWeaponLoad.keySet().removeIf(uuid -> !onlineUuids.contains(uuid));
        lastPlayerPos.keySet().removeIf(uuid -> !onlineUuids.contains(uuid));
        accumulatedDistance.keySet().removeIf(uuid -> !onlineUuids.contains(uuid));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        boolean isClientSide = player.level().isClientSide();

        // 水上行走需要在客户端和服务端都执行，否则客户端物理引擎会覆盖服务端设置的速度
        // 其他逻辑只在服务端执行
        if (!isClientSide) {
            tickEquipmentPassives(player);
            tickInventoryLoad(player);
            tickReconBodyLock(player);
        }

        boolean isTransformed = TransformationManager.isPlayerTransformed(player);

        com.piranport.debug.PiranPortDebug.event("PlayerTick | player={} transformed={}",
                player.getName().getString(), isTransformed);

        if (!isTransformed) {
            WaterWalkingHandler.clear(player);
            if (!isClientSide) {
                AASilenceManager.clear(player);
                lastPlayerPos.remove(player.getUUID());
                accumulatedDistance.remove(player.getUUID());

                // 调试日志：帮助诊断变身检测失败
                if (player.tickCount % 100 == 0 && player.isInWater()) {
                    ItemStack coreStack = TransformationManager.getCoreFromConfiguredSlot(player);
                    if (coreStack.getItem() instanceof ShipCoreItem) {
                        boolean transformed = TransformationManager.isTransformed(coreStack);
                        if (!transformed) {
                            PiranPort.LOGGER.warn("Player {} has ship core in slot but not transformed. Core: {}, Slot: {}, DataComponent: {}",
                                player.getName().getString(),
                                coreStack.getItem(),
                                ModCommonConfig.SHIP_CORE_SLOT_MODE.get(),
                                coreStack.get(ModDataComponents.SHIP_CORE_TRANSFORMED.get()));
                        }
                    }
                }
            }
            return;
        }

        // 服务端专属逻辑
        if (!isClientSide) {
            tickFuelConsumption(player);
            if (!TransformationManager.isPlayerTransformed(player)) {
                WaterWalkingHandler.clear(player);
                return;
            }
        }

        ItemStack transformedCore = TransformationManager.findTransformedCore(player);
        boolean isSubmarine = transformedCore.getItem() instanceof ShipCoreItem sci
                && sci.getShipType() == ShipType.SUBMARINE;

        com.piranport.debug.PiranPortDebug.event("WaterWalkCheck | player={} submarine={}",
                player.getName().getString(), isSubmarine);

        // 水上行走：客户端和服务端都需要执行
        WaterWalkingHandler.tick(player, isSubmarine);

        // 以下逻辑只在服务端执行
        if (!isClientSide) {
            if (transformedCore.getItem() instanceof ShipCoreItem activeCore) {
                com.piranport.combat.ShipHealthOverride.apply(
                        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH),
                        activeCore.getShipType().maxHealth());
                if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
            }
            tickSubmarineEffects(player, isSubmarine);
            ShipCoreCombat.tickCannonAutoReload(player, transformedCore);
            tickSonarGlow(player, transformedCore);
            tickCleanupResidualSlowdown(player);
            tickAutoCombatIfNeeded(player);
            // 决策/数值/05 §定稿修订 #3：防空静默窗口每 tick 衰减
            AASilenceManager.tickDown(player);
            // 策划决策/舰装/舰装-自动近防炮系统.md：H 键总开关在 server 侧生效时 tick 近防炮
            AutoCIWSItem.tickAutoCIWS(player, isAutoFireEnabled(transformedCore));
        }
    }

    /** 麒麟头巾隐身 + 足球套装经验加成 */
    private static void tickEquipmentPassives(Player player) {
        if (player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof KirinHeadbandItem) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 25, 0, false, false, true));
        }
        boolean hasFootball = false;
        for (ItemStack armor : player.getArmorSlots()) {
            if (armor.getItem() instanceof FootballArmorItem) {
                hasFootball = true;
                break;
            }
        }
        if (hasFootball) {
            player.addEffect(new MobEffectInstance(ModMobEffects.EXPERIENCE_BOOST, 25, 0, false, false, true));
        }
    }

    /** 检测背包武器变化并重算属性 */
    private static void tickInventoryLoad(Player player) {
        // 降低检查频率至每5tick，减少重复计算
        if (player.tickCount % INVENTORY_LOAD_CHECK_INTERVAL == 0) {
            tickInventoryLoadCheck(player);
        }
    }

    /** 侦察模式：锁定玩家身体位置，阻止移动 */
    private static void tickReconBodyLock(Player player) {
        if (ReconManager.isInRecon(player.getUUID())) {
            Vec3 vel = player.getDeltaMovement();
            double newY = vel.y > 0 ? 0 : vel.y;
            player.setDeltaMovement(0, newY, 0);
            player.xxa = 0;
            player.zza = 0;
            if (player.isInWater() && !player.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value())) {
                player.setDeltaMovement(0, 0, 0);
                player.resetFallDistance();
            }
        }
    }

    /** 近防炮、导弹和战斗机读取同一个自动模式总开关。 */
    private static boolean isAutoFireEnabled(ItemStack transformedCore) {
        return AutoModeState.fromStack(transformedCore) == AutoModeState.ON;
    }

    /** 潜艇效果：无限水下呼吸 + 水下隐身 + 深海夜视（仅在效果快过期时刷新，避免每tick发包） */
    private static void tickSubmarineEffects(Player player, boolean isSubmarine) {
        if (isSubmarine) {
            MobEffectInstance waterBreathing = player.getEffect(MobEffects.WATER_BREATHING);
            if (waterBreathing == null || waterBreathing.getDuration() <= 100) {
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 400, 0, false, false, true));
            }
            if (player.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value())) {
                MobEffectInstance invis = player.getEffect(MobEffects.INVISIBILITY);
                if (invis == null || invis.getDuration() <= 10) {
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, true));
                }
            }
            // 深海夜视：仅在深水区（Y < 50）给予
            if (player.isInWater() && player.getY() < 50) {
                MobEffectInstance nightVision = player.getEffect(MobEffects.NIGHT_VISION);
                if (nightVision == null || nightVision.getDuration() <= 60) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false, true));
                }
            }
        }
    }

    /**
     * 索敌高亮：核心被动声呐 + 已装备雷达，统一在本次扫描里处理（每40tick，错峰执行）。
     *
     * <p><b>为什么合并成一次扫描</b>：声呐和雷达都是「加 GLOWING 效果」，而 addEffect 会覆盖
     * 同名效果的持续时间。若两者各扫一次，后执行的一方会把先执行的一方的效果时长重置，
     * 造成高频重扫。这里一次取完实体再按各自条件打标，只发一次效果包。
     *
     * <p><b>为什么范围内实体只扫一次</b>：多台雷达同时装备时，扫描范围取其中最大值，
     * 逐台各扫一遍会重复遍历同一批实体。取并集后逐实体判断，性能按最大范围算一次。
     */
    private static void tickSonarGlow(Player player, ItemStack transformedCore) {
        // 错峰执行：使用UUID哈希避免玩家ID连续分配导致的碰撞
        if ((player.tickCount + player.getUUID().hashCode()) % SONAR_SCAN_INTERVAL != 0) return;

        boolean hasSonar = TransformationManager.hasSonarEquipped(player, transformedCore);
        List<RadarItem> radars = TransformationManager.getEquippedRadars(transformedCore);
        if (!hasSonar && radars.isEmpty()) return;

        // 动态限制扫描范围，避免超出服务器模拟距离
        int simDist = ((ServerLevel) player.level()).getServer().getPlayerList().getSimulationDistance();
        double simLimit = simDist * 16.0 - 8.0;

        // 实际扫描半径取「所有已装备索敌设备里的最大值」，再统一受模拟距离钳制。
        // 逐台各扫一遍会重复遍历同一批实体，取并集后逐实体判断只需扫一次。
        double scanRange = 0.0;
        if (hasSonar) {
            // Phase 27：策划 §3.6 - 声呐半径按 SonarItem.radius 决定 (标准 24/改进 32/先进 40)
            scanRange = Math.max(scanRange,
                    TransformationManager.getEquippedSonarRadius(player, transformedCore));
        }
        for (RadarItem radar : radars) {
            scanRange = Math.max(scanRange, radar.getRange());
        }
        double maxRange = Math.min(scanRange, simLimit);

        AABB scanBox = player.getBoundingBox().inflate(maxRange, 8.0, maxRange);
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                LivingEntity.class, scanBox,
                e -> e.isAlive() && e != player && !(e instanceof Player));
        for (LivingEntity entity : nearby) {
            if (isDetectedBySonarOrRadar(player, entity, hasSonar, radars)) {
                entity.addEffect(new MobEffectInstance(
                        MobEffects.GLOWING, SONAR_SCAN_INTERVAL, 0, false, false, false));
            }
        }
    }

    /**
     * 判定单个实体是否被核心被动声呐或任一已装备雷达探测到。
     * 被动声呐沿用策划决策/火控/02 的口径：范围内一切非玩家生物（含友方水生生物）都标记；
     * 雷达则严格按各自「索敌目标」分层，三种分层互斥。
     */
    private static boolean isDetectedBySonarOrRadar(Player player, LivingEntity entity,
                                                    boolean hasSonar, List<RadarItem> radars) {
        // 被动声呐：全目标
        if (hasSonar) return true;
        if (radars.isEmpty()) return false;

        // 先算出目标的分层归属，避免每台雷达重复调用 isUnderWater / onGround 等
        boolean underwater = isUnderwaterTarget(entity);
        boolean airborne = isAirborneTarget(entity);
        for (RadarItem radar : radars) {
            // 各雷达索敌范围独立：装了大范围雷达也要能让小范围雷达的高亮不越界
            if (radar.getRange() < player.distanceTo(entity)) continue;
            switch (radar.getTarget()) {
                case SUBMARINE -> {
                    if (underwater) return true;
                }
                case AIR -> {
                    if (airborne) return true;
                }
                // 对海：既不在水下、也不在空中（水面舰船、岸上单位）
                case SURFACE -> {
                    if (!underwater && !airborne) return true;
                }
            }
        }
        return false;
    }

    /**
     * 水下目标判定 —— 沿用反潜机 ASW 的口径（见 AircraftAswRecon.isAswTarget），
     * 但去掉敌对过滤：索敌是「发现」而非「攻击」，友方水下生物同样该被雷达看到。
     */
    private static boolean isUnderwaterTarget(LivingEntity entity) {
        if (entity instanceof com.piranport.npc.deepocean.DeepOceanSubmarineEntity) return true;
        if (entity.getType().is(net.minecraft.tags.EntityTypeTags.AQUATIC)) return true;
        if (entity instanceof net.minecraft.world.entity.monster.Guardian) return true;
        return entity.isUnderWater();
    }

    /**
     * 飞行目标判定 —— 与防空导弹的判定（MissileEntity.isValidTarget）保持一致：
     * 离地且不在水中即视为空中，或正在上升（避免把下落中的地面实体误判为空中目标）。
     *
     * <p>为什么不在这里判断 {@code AircraftEntity}：AircraftEntity 继承自 Entity 而非
     * LivingEntity，根本进不了本次扫描的实体列表（扫描按 LivingEntity 过滤，见 tickSonarGlow）。
     * 而且飞机升空后 onGround() 恒为 false，上面的条件已经覆盖，额外分支是死代码。
     */
    private static boolean isAirborneTarget(LivingEntity entity) {
        return (!entity.onGround() && !entity.isInWater()) || entity.getDeltaMovement().y > 0.1;
    }

    /** 清除侦察模式残留的减速效果（每20tick） */
    private static void tickCleanupResidualSlowdown(Player player) {
        if (player.tickCount % SLOWNESS_CLEANUP_INTERVAL != 0) return;
        MobEffectInstance slowness = player.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slowness != null && slowness.getAmplifier() >= 9
                && !ReconManager.isInRecon(player.getUUID())) {
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
    }

    /** 战斗机自动升空 + 防空导弹（每40tick，错峰执行） */
    private static void tickAutoCombatIfNeeded(Player player) {
        // 错峰执行：使用UUID哈希避免玩家ID连续分配导致的碰撞
        if ((player.tickCount + player.getUUID().hashCode()) % AUTO_COMBAT_INTERVAL == 0) {
            tickAutoCombat(player);
        }
    }

    /** 查找变身后的核心并执行自动战斗 */
    private static void tickAutoCombat(Player player) {
        ItemStack autoLaunchCore = TransformationManager.findTransformedCore(player);
        if (autoLaunchCore.isEmpty()) return;

        AutoModeState mode = AutoModeState.fromStack(autoLaunchCore);
        if (mode == AutoModeState.OFF) return;

        int autoLaunchSlot = -1;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            if (s == autoLaunchCore || (s.getItem() instanceof ShipCoreItem && ItemStack.isSameItemSameComponents(s, autoLaunchCore))) {
                autoLaunchSlot = i; break;
            }
        }
        if (autoLaunchSlot == -1) {
            ItemStack oh = inv.offhand.get(0);
            if (oh == autoLaunchCore || (oh.getItem() instanceof ShipCoreItem && ItemStack.isSameItemSameComponents(oh, autoLaunchCore))) {
                autoLaunchSlot = 40;
            }
        }
        if (autoLaunchSlot >= 0) {
            // 起飞可刷新防空静默窗口，但静默本身不阻塞起飞动作。
            tickAutoLaunchFighters(player, autoLaunchCore, autoLaunchSlot);
            tickAntiAirMissiles(player, autoLaunchCore, autoLaunchSlot);
        }
    }

    /**
     * 通过配置的槽位驱动变身。
     * 配置槽位放入核心 → 自动变身；移除核心 → 自动解除；核心不动 → 仅武器变化时重算属性。
     */
    private static void tickInventoryLoadCheck(Player player) {
        Inventory inv = player.getInventory();
        ItemStack coreStack = TransformationManager.getCoreFromConfiguredSlot(player);
        boolean hasCoreEquipped = coreStack.getItem() instanceof ShipCoreItem;

        if (hasCoreEquipped) {
            if (!TransformationManager.isTransformed(coreStack)) {
                FuelData fuel = coreStack.getOrDefault(ModDataComponents.SHIP_CORE_FUEL.get(),
                        new FuelData(0, ((ShipCoreItem) coreStack.getItem()).getShipType().fuelCapacity));
                if (fuel.isEmpty()) {
                    // 燃料不足提示（聊天框 + 动作栏）
                    Integer cached = lastWeaponLoad.get(player.getUUID());
                    if (cached == null || cached != -999) {
                        lastWeaponLoad.put(player.getUUID(), -999);
                        player.sendSystemMessage(
                            Component.translatable("message.piranport.no_fuel_enhanced"));
                        player.displayClientMessage(
                            Component.translatable("message.piranport.no_fuel"), true);
                    }
                    return;
                }
                Integer cached = lastWeaponLoad.get(player.getUUID());
                if (cached != null && cached == -999) {
                    lastWeaponLoad.remove(player.getUUID());
                }
                TransformationManager.setTransformedAndWriteBack(player, coreStack, true);
                // 重新读取核心以获取最新的ItemStack引用（DataComponents可能创建新实例）
                coreStack = TransformationManager.getCoreFromConfiguredSlot(player);

                com.piranport.debug.PiranPortDebug.event("Transform ON | player={} core={} transformed={}",
                    player.getName().getString(),
                    coreStack.getItem().getClass().getSimpleName(),
                    TransformationManager.isTransformed(coreStack));

                // 发布变身激活事件 (监听器会处理属性/消息/粒子)
                com.piranport.event.EventBus.getInstance().post(
                        new com.piranport.event.TransformationEvent(player, coreStack, true));

                // 非监听器处理的逻辑保留在这里
                ShipCoreCombat.refillAircraftFuel(player, coreStack);
                lastWeaponLoad.put(player.getUUID(), -1);
                return;
            }

            int weaponLoad = TransformationManager.getInventoryWeaponLoad(inv);
            int armorLoad  = TransformationManager.getCoreArmorLoad(coreStack);
            double engineBonus = TransformationManager.getCoreEngineSpeedBonus(coreStack);
            int maxLoad    = ((ShipCoreItem) coreStack.getItem()).getShipType().maxLoad;
            int cacheKey   = java.util.Objects.hash(weaponLoad, armorLoad, maxLoad, engineBonus);
            Integer cached = lastWeaponLoad.get(player.getUUID());
            if (cached == null || cached != cacheKey) {
                lastWeaponLoad.put(player.getUUID(), cacheKey);
                TransformationManager.applyTransformationAttributes(player, coreStack);
            } else {
                int totalLoad = weaponLoad + armorLoad;
                if (totalLoad > maxLoad && player.tickCount % 40 == 0) {
                    TransformationManager.applyOverweightPenalty(player, totalLoad, maxLoad);
                }
            }
        } else {
            for (int i = 0; i < inv.items.size(); i++) {
                ItemStack stack = inv.items.get(i);
                if (stack.getItem() instanceof ShipCoreItem
                        && TransformationManager.isTransformed(stack)) {
                    TransformationManager.setTransformed(stack, false);
                    // P1修复: 显式写回以确保状态同步（DataComponents可能创建新ItemStack）
                    inv.items.set(i, stack);
                }
            }
            if (lastWeaponLoad.remove(player.getUUID()) != null) {
                // 发布变身解除事件 (监听器会处理属性/消息)
                ItemStack core = TransformationManager.findTransformedCore(player);
                if (!core.isEmpty()) {
                    com.piranport.event.EventBus.getInstance().post(
                            new com.piranport.event.TransformationEvent(player, core, false));
                }

                // 非监听器处理的逻辑保留在这里
                player.removeEffect(MobEffects.WATER_BREATHING);
                PlayerAircraftHelper.recallAircraftForPlayer(player);
            }
        }
    }

    /** 燃料消耗：基于移动距离，耗尽时自动解除变身（每5tick计算一次） */
    private static void tickFuelConsumption(Player player) {
        // 性能优化：每5tick计算一次燃料消耗
        if (player.tickCount % 5 != 0) return;

        UUID uuid = player.getUUID();
        Vec3 currentPos = player.position();
        Vec3 lastPos = lastPlayerPos.put(uuid, currentPos);
        if (lastPos == null) return;

        double dist = currentPos.distanceTo(lastPos);
        // P1修复: 降低传送检测阈值至32格，避免误判高速鞘翅飞行
        final double TELEPORT_THRESHOLD = 32.0;
        if (dist > TELEPORT_THRESHOLD || player.isFallFlying()) {
            lastPlayerPos.put(uuid, currentPos);
            accumulatedDistance.remove(uuid);
            return;
        }
        if (dist < 0.001) return;

        ItemStack core = TransformationManager.findTransformedCore(player);
        if (!(core.getItem() instanceof ShipCoreItem sci)) return;

        double threshold = sci.getShipType().distancePerFuel;
        double acc = accumulatedDistance.getOrDefault(uuid, 0.0) + dist;
        FuelData fuel = core.getOrDefault(ModDataComponents.SHIP_CORE_FUEL.get(),
                new FuelData(0, sci.getShipType().fuelCapacity));

        while (acc >= threshold && fuel.currentFuel() > 0) {
            acc -= threshold;
            fuel = fuel.withCurrentFuel(fuel.currentFuel() - 1);
        }
        core.set(ModDataComponents.SHIP_CORE_FUEL.get(), fuel);

        // 写回槽位以确保燃料数据同步
        TransformationManager.writeCoreToConfiguredSlot(player, core);

        if (fuel.isEmpty()) {
            cleanupPlayerState(uuid);
            TransformationManager.setTransformedAndWriteBack(player, core, false);

            // 发布燃料耗尽事件
            com.piranport.event.EventBus.getInstance().post(
                    new com.piranport.event.FuelDepletedEvent(player, core));

            // 发布变身解除事件
            com.piranport.event.EventBus.getInstance().post(
                    new com.piranport.event.TransformationEvent(player, core, false));

            // 非监听器处理的逻辑
            PlayerAircraftHelper.recallAircraftForPlayer(player);
            player.displayClientMessage(
                    Component.translatable("message.piranport.fuel_depleted"), true);
            return;
        }

        accumulatedDistance.put(uuid, acc);
    }

    /** 清理玩家状态缓存（燃料耗尽、登出等场景统一调用）*/
    private static void cleanupPlayerState(UUID uuid) {
        lastWeaponLoad.remove(uuid);
        accumulatedDistance.remove(uuid);
        lastPlayerPos.remove(uuid);
    }

    /** 自动发射战斗机锁定附近飞行敌对生物 */
    private static void tickAutoLaunchFighters(Player player, ItemStack coreStack, int coreSlot) {
        // P2-8: PERF 埋点 — 火控解算（每 tick 遍历目标）
        boolean perfEnabled = com.piranport.debug.PiranPortDebug.isServerEnabled();
        long t0 = perfEnabled ? System.nanoTime() : 0L;

        List<LivingEntity> flyingHostiles = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(64.0),
                e -> e.isAlive() && e instanceof Enemy
                        && (e instanceof FlyingMob || e instanceof Phantom || e instanceof Vex));

        if (flyingHostiles.isEmpty()) {
            if (perfEnabled) com.piranport.debug.PiranPortDebug.perf("FireControlSolve", System.nanoTime() - t0,
                    "player=" + player.getName().getString() + " targets=0");
            return;
        }

        if (player.level() instanceof ServerLevel sl) {
            List<UUID> currentLocks = FireControlManager.getTargets(player.getUUID());
            boolean hasActiveLock = currentLocks.stream()
                    .map(sl::getEntity)
                    .anyMatch(e -> e != null && e.isAlive());
            if (!hasActiveLock) {
                LivingEntity nearest = flyingHostiles.stream()
                        .min(Comparator.comparingDouble(player::distanceTo))
                        .orElse(null);
                if (nearest != null) {
                    // 自动火控只追加，不替换玩家用 O 积累的目标列表
                    FireControlManager.addTarget(player.getUUID(), nearest.getUUID());
                }
            }
        }
        ShipCoreCombat.tryAutoLaunchFighter(player.level(), player, coreStack, coreSlot);

        if (perfEnabled) com.piranport.debug.PiranPortDebug.perf("FireControlSolve", System.nanoTime() - t0,
                "player=" + player.getName().getString() + " targets=" + flyingHostiles.size());
    }

    /** 防空导弹：检测32格内空中敌对目标 */
    private static void tickAntiAirMissiles(Player player, ItemStack coreStack, int coreSlot) {
        if (AASilenceManager.isSilenced(player)) return;
        boolean hasAirborneHostile = !player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(32.0),
                e -> {
                    if (!e.isAlive() || !e.isPickable() || !(e instanceof Enemy) || e.isUnderWater()) return false;
                    BlockPos below = e.blockPosition().below(2);
                    return !e.onGround() && !player.level().getBlockState(below).isSolid();
                }).isEmpty();
        if (hasAirborneHostile) {
            ShipCoreCombat.tryAutoFireAntiAirMissile(player.level(), player, coreStack, coreSlot);
        }
    }
}
