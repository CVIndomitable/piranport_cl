package com.piranport.handler;

import com.piranport.item.ShipType;

import com.piranport.PiranPort;
import com.piranport.aviation.FireControlManager;
import com.piranport.aviation.ReconManager;
import com.piranport.combat.TransformationManager;
import com.piranport.component.FuelData;
import com.piranport.config.ModCommonConfig;
import net.minecraft.nbt.CompoundTag;

import com.piranport.item.KirinHeadbandItem;
import com.piranport.item.FootballArmorItem;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
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
 * 玩家 Tick 处理器 — 服务端每 tick 驱动燃料消耗、水上行走、声呐、自动战斗等。
 *
 * <p><b>线程模型</b>: 服务端主线程，使用 ConcurrentHashMap 防御服务器关闭时的竞态条件。
 * <p><b>缓存生命周期</b>:
 *   {@link #lastWeaponLoad} / {@link #lastPlayerPos} / {@link #accumulatedDistance} —
 *   在 {@link #onPlayerLogout(UUID)} 中清理以单向释放内存，
 *   全局清理通过 {@link #clearCaches()} 在 {@link com.piranport.server.ServerGameEvents#onServerStopped} 中调用。
 * <p><b>访问限制</b>: 仅在服务端运行，客户端不会触发。
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
    /** 玩家 UUID → 上次计算的 yaw 角度。用于缓存水面行走的三角函数计算。 */
    private static final Map<UUID, Float> lastYaw = new ConcurrentHashMap<>();
    /** 玩家 UUID → 缓存的方向向量。用于水面行走加速。 */
    private static final Map<UUID, Vec3> cachedDirection = new ConcurrentHashMap<>();
    /** 玩家 UUID → 水面 Y 坐标。用于水面行走位置锁定，防止下沉。 */
    private static final Map<UUID, Double> waterSurfaceY = new ConcurrentHashMap<>();
    /** 玩家 UUID → 上次离开水面的tick。用于延迟清理水面Y缓存。 */
    private static final Map<UUID, Integer> lastWaterExitTick = new ConcurrentHashMap<>();
    /** 清理所有缓存（服务器关闭时调用）*/
    public static void clearCaches() {
        lastWeaponLoad.clear();
        lastPlayerPos.clear();
        accumulatedDistance.clear();
        lastYaw.clear();
        cachedDirection.clear();
        waterSurfaceY.clear();
        lastWaterExitTick.clear();
    }

    /** 玩家登出时清理该玩家的缓存条目，防止长时间运行内存泄漏 */
    public static void onPlayerLogout(UUID uuid) {
        lastWeaponLoad.remove(uuid);
        lastPlayerPos.remove(uuid);
        accumulatedDistance.remove(uuid);
        lastYaw.remove(uuid);
        cachedDirection.remove(uuid);
        waterSurfaceY.remove(uuid);
        lastWaterExitTick.remove(uuid);
    }

    /** 定期清理离线玩家的缓存条目，防止服务器崩溃导致的内存泄漏 */
    public static void cleanupOfflinePlayers(net.minecraft.server.MinecraftServer server) {
        java.util.Set<UUID> onlineUuids = server.getPlayerList().getPlayers().stream()
                .map(net.minecraft.world.entity.Entity::getUUID)
                .collect(java.util.stream.Collectors.toSet());

        lastWeaponLoad.keySet().removeIf(uuid -> !onlineUuids.contains(uuid));
        lastPlayerPos.keySet().removeIf(uuid -> !onlineUuids.contains(uuid));
        accumulatedDistance.keySet().removeIf(uuid -> !onlineUuids.contains(uuid));
        lastYaw.keySet().removeIf(uuid -> !onlineUuids.contains(uuid));
        cachedDirection.keySet().removeIf(uuid -> !onlineUuids.contains(uuid));
        waterSurfaceY.keySet().removeIf(uuid -> !onlineUuids.contains(uuid));
        lastWaterExitTick.keySet().removeIf(uuid -> !onlineUuids.contains(uuid));
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
            if (!isClientSide) {
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
            if (!TransformationManager.isPlayerTransformed(player)) return;
        }

        ItemStack transformedCore = TransformationManager.findTransformedCore(player);
        boolean isSubmarine = transformedCore.getItem() instanceof ShipCoreItem sci
                && sci.getShipType() == ShipType.SUBMARINE;

        com.piranport.debug.PiranPortDebug.event("WaterWalkCheck | player={} submarine={}",
                player.getName().getString(), isSubmarine);

        // 水上行走：客户端和服务端都需要执行
        handleWaterWalkingIfNeeded(player, isSubmarine);

        // 以下逻辑只在服务端执行
        if (!isClientSide) {
            tickSubmarineEffects(player, isSubmarine);
            ShipCoreCombat.tickCannonAutoReload(player, transformedCore);
            tickSonarGlow(player, transformedCore);
            tickCleanupResidualSlowdown(player);
            tickAutoCombatIfNeeded(player);
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

    /** 水面行走条件判断后委托给 handleWaterWalking */
    private static void handleWaterWalkingIfNeeded(Player player, boolean isSubmarine) {
        if (isSubmarine) {
            return;
        }

        boolean inWater = player.isInWater();
        boolean eyeInWater = player.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value());

        if (inWater && eyeInWater) {
            applyUnderwaterBuoyancy(player);
            waterSurfaceY.remove(player.getUUID());
            return;
        }

        if (inWater) {
            Vec3 vel = player.getDeltaMovement();

            if (vel.y < 0) {
                player.setDeltaMovement(vel.x, 0.0, vel.z);
            }

            // 方法2：如果还在下沉，强制拉回位置（更强力）
            // 记录水面Y坐标
            UUID uuid = player.getUUID();
            Double surfaceY = waterSurfaceY.get(uuid);
            double currentY = player.getY();

            if (surfaceY == null) {
                // 第一次进入水面，记录当前位置
                waterSurfaceY.put(uuid, currentY);
            } else if (currentY < surfaceY - 0.1) {
                // 如果下沉超过0.1格，强制拉回
                player.setPos(player.getX(), surfaceY, player.getZ());
                player.setDeltaMovement(vel.x, 0.0, vel.z);
            }

            player.resetFallDistance();

            // 水平移动加速
            handleWaterWalking(player);
        } else {
            // 离开水面，清除记录
            waterSurfaceY.remove(player.getUUID());
        }
    }

    private static void applyUnderwaterBuoyancy(Player player) {
        Vec3 vel = player.getDeltaMovement();
        double buoyancy = ModCommonConfig.WATER_SURFACE_BUOYANCY.get();
        double maxRiseSpeed = Math.max(0.05, Math.min(0.6, buoyancy));
        double rise = Math.min(maxRiseSpeed, Math.max(vel.y, 0.0) + buoyancy * 0.08);
        player.setDeltaMovement(vel.x, rise, vel.z);
        player.resetFallDistance();
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

    /** 声纳效果：24格内水生/敌对生物发光（每40tick，错峰执行） */
    private static void tickSonarGlow(Player player, ItemStack transformedCore) {
        // 错峰执行：使用UUID哈希避免玩家ID连续分配导致的碰撞
        if ((player.tickCount + player.getUUID().hashCode()) % SONAR_SCAN_INTERVAL != 0) return;
        if (!TransformationManager.hasSonarEquipped(player, transformedCore)) return;

        // 动态限制扫描范围，避免超出服务器模拟距离
        int simDist = ((ServerLevel) player.level()).getServer().getPlayerList().getSimulationDistance();
        double maxRange = Math.min(24.0, simDist * 16.0 - 8.0);

        AABB scanBox = player.getBoundingBox().inflate(maxRange, 8.0, maxRange);
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                LivingEntity.class, scanBox,
                e -> e.isAlive() && e != player && !(e instanceof Player));
        for (LivingEntity entity : nearby) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, SONAR_SCAN_INTERVAL, 0, false, false, false));
        }
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
        if (!autoLaunchCore.getOrDefault(ModDataComponents.SHIP_AUTO_LAUNCH.get(), false)) return;

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

    /** 水面行走：取消下沉、水平加速补偿 */
    private static void handleWaterWalking(Player player) {
        Vec3 vel = player.getDeltaMovement();
        if (vel.y < 0) {
            player.setDeltaMovement(vel.x, 0.0, vel.z);
        }
        player.resetFallDistance();

        double accel = ModCommonConfig.WATER_WALKING_ACCELERATION.get();
        if (accel <= 0.0001) return;

        Vec3 currentVel = player.getDeltaMovement();
        float inputX = player.xxa;
        float inputZ = player.zza;
        boolean hasInput = Math.abs(inputX) > 0.01f || Math.abs(inputZ) > 0.01f;

        if (hasInput) {
            float yaw = player.getYRot();
            UUID uuid = player.getUUID();

            // 缓存三角函数计算：仅在yaw变化超过5度时重算
            Float cachedYaw = lastYaw.get(uuid);
            Vec3 direction = cachedDirection.get(uuid);
            if (cachedYaw == null || Math.abs(yaw - cachedYaw) > 5.0f || direction == null) {
                float yawRad = yaw * ((float) Math.PI / 180f);
                double sinYaw = Math.sin(yawRad);
                double cosYaw = Math.cos(yawRad);
                direction = new Vec3(-sinYaw, 0, cosYaw);
                lastYaw.put(uuid, yaw);
                cachedDirection.put(uuid, direction);
            }

            double dirX = direction.x * inputZ + direction.z * inputX;
            double dirZ = direction.z * inputZ - direction.x * inputX;
            double dirLen = Math.sqrt(dirX * dirX + dirZ * dirZ);
            if (dirLen > 0.001) {
                dirX /= dirLen;
                dirZ /= dirLen;
                player.setDeltaMovement(
                    currentVel.x + dirX * accel,
                    currentVel.y,
                    currentVel.z + dirZ * accel
                );
            }
        } else {
            double horizontalSpeed = Math.sqrt(currentVel.x * currentVel.x + currentVel.z * currentVel.z);
            if (horizontalSpeed > 0.001) {
                double deceleration = ModCommonConfig.WATER_WALKING_DECELERATION.get();
                player.setDeltaMovement(
                    currentVel.x * deceleration,
                    currentVel.y,
                    currentVel.z * deceleration
                );
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
        lastYaw.remove(uuid);
        cachedDirection.remove(uuid);
        waterSurfaceY.remove(uuid);
    }

    /** 自动发射战斗机锁定附近飞行敌对生物 */
    private static void tickAutoLaunchFighters(Player player, ItemStack coreStack, int coreSlot) {
        List<LivingEntity> flyingHostiles = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(64.0),
                e -> e.isAlive() && e instanceof Enemy
                        && (e instanceof FlyingMob || e instanceof Phantom || e instanceof Vex));

        if (flyingHostiles.isEmpty()) return;

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
                    FireControlManager.lock(player.getUUID(), nearest.getUUID());
                }
            }
        }
        ShipCoreCombat.tryAutoLaunchFighter(player.level(), player, coreStack, coreSlot);
    }

    /** 防空导弹：检测32格内空中敌对目标 */
    private static void tickAntiAirMissiles(Player player, ItemStack coreStack, int coreSlot) {
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
