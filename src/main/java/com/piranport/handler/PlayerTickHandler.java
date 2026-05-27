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
    /** 无GUI模式负重检查间隔（tick）- 降低频率以提升性能 */
    private static final int INVENTORY_LOAD_CHECK_INTERVAL = 10;
    /** 传送检测距离阈值（格）- 降低以避免误判鞘翅飞行 */
    private static final double TELEPORT_DETECTION_THRESHOLD = 64.0;

    // ==================== 缓存 Maps ====================
    /** 玩家 UUID → 上次背包武器总载重。用于无GUI模式下的属性重算检测。 */
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
    /** 缓存的配置值：是否启用舰核GUI（延迟初始化，避免配置加载顺序问题）*/
    private static Boolean cachedShipCoreGuiEnabled = null;

    /** 获取舰核GUI配置（延迟初始化） */
    private static boolean isShipCoreGuiEnabled() {
        if (cachedShipCoreGuiEnabled == null) {
            cachedShipCoreGuiEnabled = ModCommonConfig.isShipCoreGuiEnabled();
        }
        return cachedShipCoreGuiEnabled;
    }

    /** 清理所有缓存（服务器关闭时调用）*/
    public static void clearCaches() {
        lastWeaponLoad.clear();
        lastPlayerPos.clear();
        accumulatedDistance.clear();
        lastYaw.clear();
        cachedDirection.clear();
        waterSurfaceY.clear();
        lastWaterExitTick.clear();
        cachedShipCoreGuiEnabled = null;
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
        if (player.level().isClientSide()) return;

        tickEquipmentPassives(player);
        tickInventoryLoadIfNoGui(player);
        tickReconBodyLock(player);

        if (!TransformationManager.isPlayerTransformed(player)) {
            lastPlayerPos.remove(player.getUUID());
            accumulatedDistance.remove(player.getUUID());

            // 调试日志：帮助诊断变身检测失败
            if (player.tickCount % 100 == 0 && player.isInWater()) {
                ItemStack coreStack = TransformationManager.getCoreFromConfiguredSlot(player);
                if (coreStack.getItem() instanceof ShipCoreItem) {
                    boolean transformed = TransformationManager.isTransformed(coreStack);
                    if (!transformed) {
                        PiranPort.LOGGER.warn("Player {} has ship core in slot but not transformed. Core item: {}",
                            player.getName().getString(), coreStack.getItem());
                    }
                }
            }
            return;
        }

        tickFuelConsumption(player);
        if (!TransformationManager.isPlayerTransformed(player)) return;

        ItemStack transformedCore = TransformationManager.findTransformedCore(player);
        boolean isSubmarine = transformedCore.getItem() instanceof ShipCoreItem sci
                && sci.getShipType() == ShipType.SUBMARINE;

        handleWaterWalkingIfNeeded(player, isSubmarine);
        tickSubmarineEffects(player, isSubmarine);
        tickSonarGlow(player, transformedCore);
        tickCleanupResidualSlowdown(player);
        tickAutoCombatIfNeeded(player);
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

    /** 无GUI模式：检测背包武器变化并重算属性 */
    private static void tickInventoryLoadIfNoGui(Player player) {
        // 降低检查频率至每5tick，减少重复计算
        if (!isShipCoreGuiEnabled() && player.tickCount % INVENTORY_LOAD_CHECK_INTERVAL == 0) {
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
        if (isSubmarine) return;
        UUID uuid = player.getUUID();

        if (!player.isInWater()) {
            // 延迟清理缓存：离开水面后保留5秒，防止玩家短暂跳出水面时丢失位置记录
            Integer lastWaterTick = lastWaterExitTick.get(uuid);
            if (lastWaterTick == null) {
                lastWaterExitTick.put(uuid, player.tickCount);
            } else if (player.tickCount - lastWaterTick > 100) {  // 5秒后清理
                waterSurfaceY.remove(uuid);
                lastWaterExitTick.remove(uuid);
            }
            return;
        } else {
            // 在水中：清除离开水面的记录
            lastWaterExitTick.remove(uuid);
        }

        applyWaterSurfaceControl(player);
        if (!player.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value())) {
            handleWaterWalking(player);
        }
    }

    /** 潜艇效果：无限水下呼吸 + 水下隐身 */
    private static void tickSubmarineEffects(Player player, boolean isSubmarine) {
        if (isSubmarine) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 400, 0, false, false, true));
            if (player.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value())) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, true));
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
            if (inv.items.get(i) == autoLaunchCore) { autoLaunchSlot = i; break; }
        }
        if (autoLaunchSlot == -1 && inv.offhand.get(0) == autoLaunchCore) autoLaunchSlot = 40;

        tickAutoLaunchFighters(player, autoLaunchCore, autoLaunchSlot);
        tickAntiAirMissiles(player, autoLaunchCore, autoLaunchSlot);
    }

    /**
     * 无GUI模式：通过配置的槽位驱动变身。
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
                TransformationManager.applyTransformationAttributes(player, coreStack);
                ShipCoreCombat.refillAircraftFuel(player, coreStack);
                player.displayClientMessage(
                        Component.translatable("message.piranport.transformed"), true);
                if (player.level() instanceof ServerLevel sl) {
                    double px = player.getX();
                    double py = player.getY() + 0.5;
                    double pz = player.getZ();
                    for (int i = 0; i < 30; i++) {
                        double ox = (player.getRandom().nextDouble() - 0.5) * 1.5;
                        double oy = player.getRandom().nextDouble() * 2.0;
                        double oz = (player.getRandom().nextDouble() - 0.5) * 1.5;
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                px + ox, py + oy, pz + oz,
                                1, 0, 0, 0, 0);
                    }
                }
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
                    inv.items.set(i, stack);  // 写回背包槽位
                }
            }
            if (lastWeaponLoad.remove(player.getUUID()) != null) {
                TransformationManager.removeTransformationAttributes(player);
                TransformationManager.removeOverweightPenalty(player);
                player.removeEffect(MobEffects.WATER_BREATHING);
                PlayerAircraftHelper.recallAircraftForPlayer(player);
                player.displayClientMessage(
                        Component.translatable("message.piranport.untransformed"), true);
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

    /** 水面位置控制：防止下沉 + 上浮，同时修正位置和速度 */
    private static void applyWaterSurfaceControl(Player player) {
        UUID uuid = player.getUUID();
        Vec3 vel = player.getDeltaMovement();
        double buoyancy = ModCommonConfig.WATER_SURFACE_BUOYANCY.get();

        if (player.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value())) {
            // 眼睛在水下：强上推 + 记录水面位置
            player.setDeltaMovement(vel.x, buoyancy, vel.z);
            player.resetFallDistance();

            // 只在Y值更高时更新水面位置（防止下沉时错误记录低位置）
            double currentY = player.getY();
            double cachedY = waterSurfaceY.getOrDefault(uuid, currentY);
            if (currentY > cachedY || cachedY - currentY > 2.0) {
                // 更新条件：当前位置更高，或缓存值明显过高（说明传送/跳跃）
                waterSurfaceY.put(uuid, currentY);
            }

            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,
                    player.getX(), player.getY(), player.getZ(),
                    3, 0.3, 0.5, 0.3, 0.02);
            }
        } else {
            // 眼睛在水面之上：阻止下沉 + 位置锁定
            double currentY = player.getY();
            double surfaceY = waterSurfaceY.computeIfAbsent(uuid, k -> currentY);

            if (vel.y <= 0 && currentY < surfaceY - 0.15) {
                // 正在下沉且明显低于水面：强制拉回水面位置
                player.setPos(player.getX(), surfaceY, player.getZ());
                player.setDeltaMovement(vel.x, 0, vel.z);
                player.resetFallDistance();
            } else if (vel.y < 0) {
                // 在水面附近但有下沉速度：清零下沉速度
                player.setDeltaMovement(vel.x, 0, vel.z);
                player.resetFallDistance();
            }
            // vel.y > 0（跳跃中）：不锁定 Y，让玩家正常跳起

            // 更新水面Y为当前较高值（玩家可能跳起后落回）
            if (currentY > surfaceY) {
                waterSurfaceY.put(uuid, currentY);
            }
        }
    }

    /** 燃料消耗：基于移动距离，耗尽时自动解除变身（每5tick计算一次） */
    /** 燃料消耗：基于移动距离，耗尽时自动解除变身（每5tick计算一次） */
    private static void tickFuelConsumption(Player player) {
        // 性能优化：每5tick计算一次燃料消耗
        if (player.tickCount % 5 != 0) return;

        UUID uuid = player.getUUID();
        Vec3 currentPos = player.position();
        Vec3 lastPos = lastPlayerPos.put(uuid, currentPos);
        if (lastPos == null) return;

        double dist = currentPos.distanceTo(lastPos);
        // 传送检测：降低阈值至64格，避免误判鞘翅飞行或末影珍珠
        if (dist > TELEPORT_DETECTION_THRESHOLD || player.isFallFlying()) {
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
        String slotMode = ModCommonConfig.SHIP_CORE_SLOT_MODE.get();
        if ("helmet".equalsIgnoreCase(slotMode) || "chest".equalsIgnoreCase(slotMode)) {
            player.setItemSlot(EquipmentSlot.HEAD, core);
        } else {
            player.getInventory().offhand.set(0, core);
        }

        if (fuel.isEmpty()) {
            cleanupPlayerState(uuid);
            TransformationManager.setTransformedAndWriteBack(player, core, false);
            TransformationManager.removeTransformationAttributes(player);
            TransformationManager.removeOverweightPenalty(player);
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
