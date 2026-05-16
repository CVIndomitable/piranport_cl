package com.piranport.handler;

import com.piranport.item.ShipType;

import com.piranport.PiranPort;
import com.piranport.aviation.FireControlManager;
import com.piranport.aviation.ReconManager;
import com.piranport.combat.TransformationManager;
import com.piranport.component.FuelData;
import com.piranport.config.ModCommonConfig;

import com.piranport.item.KirinHeadbandItem;
import com.piranport.item.FootballArmorItem;
import com.piranport.item.ShipCoreItem;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class PlayerTickHandler {

    // ==================== 缓存 Maps ====================
    private static final Map<UUID, Integer> lastWeaponLoad = new HashMap<>();
    private static final Map<UUID, Vec3> lastPlayerPos = new HashMap<>();
    private static final Map<UUID, Double> accumulatedDistance = new HashMap<>();

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
        if (!ModCommonConfig.isShipCoreGuiEnabled()) {
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
        if (!isSubmarine && player.isInWater() && !player.isEyeInFluidType(NeoForgeMod.WATER_TYPE.value())) {
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

    /** 声纳效果：24格内水生/敌对生物发光（每40tick） */
    private static void tickSonarGlow(Player player, ItemStack transformedCore) {
        if (player.tickCount % 40 != 0) return;
        if (!TransformationManager.hasSonarEquipped(player, transformedCore)) return;
        AABB scanBox = player.getBoundingBox().inflate(24.0, 8.0, 24.0);
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                LivingEntity.class, scanBox,
                e -> e.isAlive() && e != player && !(e instanceof Player));
        for (LivingEntity entity : nearby) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, false));
        }
    }

    /** 清除侦察模式残留的减速效果（每20tick） */
    private static void tickCleanupResidualSlowdown(Player player) {
        if (player.tickCount % 20 != 0) return;
        MobEffectInstance slowness = player.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slowness != null && slowness.getAmplifier() >= 9
                && !ReconManager.isInRecon(player.getUUID())) {
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
    }

    /** 战斗机自动升空 + 防空导弹（每40tick） */
    private static void tickAutoCombatIfNeeded(Player player) {
        if (player.tickCount % 40 == 0) {
            tickAutoCombat(player);
        }
    }

    /** 查找变身后的核心并执行自动战斗 */
    private static void tickAutoCombat(Player player) {
        ItemStack autoLaunchCore = ItemStack.EMPTY;
        int autoLaunchSlot = -1;
        ItemStack mh = player.getMainHandItem();
        Inventory inv = player.getInventory();
        if (mh.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(mh)) {
            autoLaunchCore = mh;
            autoLaunchSlot = player.getInventory().selected;
        } else {
            for (int i = 0; i < inv.items.size(); i++) {
                ItemStack s = inv.items.get(i);
                if (s.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(s)) {
                    autoLaunchCore = s;
                    autoLaunchSlot = i;
                    break;
                }
            }
            if (autoLaunchCore.isEmpty()) {
                ItemStack offh = inv.offhand.get(0);
                if (offh.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(offh)) {
                    autoLaunchCore = offh;
                    autoLaunchSlot = 40;
                }
            }
        }
        if (autoLaunchCore.isEmpty()) return;
        if (!autoLaunchCore.getOrDefault(ModDataComponents.SHIP_AUTO_LAUNCH.get(), false)) return;

        tickAutoLaunchFighters(player, autoLaunchCore, autoLaunchSlot);
        tickAntiAirMissiles(player, autoLaunchCore, autoLaunchSlot);
    }

    /**
     * 无GUI模式：通过副手槽驱动变身。
     * 副手放入核心 → 自动变身；副手移除核心 → 自动解除；核心不动 → 仅武器变化时重算属性。
     */
    private static void tickInventoryLoadCheck(Player player) {
        Inventory inv = player.getInventory();
        ItemStack offhand = inv.offhand.get(0);
        boolean coreInOffhand = offhand.getItem() instanceof ShipCoreItem;

        if (coreInOffhand) {
            if (!TransformationManager.isTransformed(offhand)) {
                FuelData fuel = offhand.getOrDefault(ModDataComponents.SHIP_CORE_FUEL.get(),
                        new FuelData(0, ((ShipCoreItem) offhand.getItem()).getShipType().fuelCapacity));
                if (fuel.isEmpty()) {
                    Integer cached = lastWeaponLoad.get(player.getUUID());
                    if (cached == null || cached != -999) {
                        lastWeaponLoad.put(player.getUUID(), -999);
                        player.displayClientMessage(
                                Component.translatable("message.piranport.no_fuel"), true);
                    }
                    return;
                }
                Integer cached = lastWeaponLoad.get(player.getUUID());
                if (cached != null && cached == -999) {
                    lastWeaponLoad.remove(player.getUUID());
                }
                TransformationManager.setTransformed(offhand, true);
                TransformationManager.applyTransformationAttributes(player, offhand);
                ShipCoreItem.refillAircraftFuel(player, offhand);
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
            int armorLoad  = TransformationManager.getCoreArmorLoad(offhand);
            double engineBonus = TransformationManager.getCoreEngineSpeedBonus(offhand);
            int maxLoad    = ((ShipCoreItem) offhand.getItem()).getShipType().maxLoad;
            int cacheKey   = java.util.Objects.hash(weaponLoad, armorLoad, maxLoad, engineBonus);
            Integer cached = lastWeaponLoad.get(player.getUUID());
            if (cached == null || cached != cacheKey) {
                lastWeaponLoad.put(player.getUUID(), cacheKey);
                TransformationManager.applyTransformationAttributes(player, offhand);
            } else {
                int totalLoad = weaponLoad + armorLoad;
                if (totalLoad > maxLoad && player.tickCount % 40 == 0) {
                    TransformationManager.applyOverweightPenalty(player, totalLoad, maxLoad);
                }
            }
        } else {
            for (ItemStack stack : inv.items) {
                if (stack.getItem() instanceof ShipCoreItem
                        && TransformationManager.isTransformed(stack)) {
                    TransformationManager.setTransformed(stack, false);
                }
            }
            if (lastWeaponLoad.remove(player.getUUID()) != null) {
                TransformationManager.removeTransformationAttributes(player);
                TransformationManager.removeOverweightPenalty(player);
                player.removeEffect(ModMobEffects.FLAMMABLE);
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
            float yaw = player.getYRot() * ((float) Math.PI / 180f);
            double dirX = -Math.sin(yaw) * inputZ + Math.cos(yaw) * inputX;
            double dirZ = Math.cos(yaw) * inputZ + Math.sin(yaw) * inputX;
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

    /** 燃料消耗：基于移动距离，耗尽时自动解除变身 */
    private static void tickFuelConsumption(Player player) {
        UUID uuid = player.getUUID();
        Vec3 currentPos = player.position();
        Vec3 lastPos = lastPlayerPos.put(uuid, currentPos);
        if (lastPos == null) return;

        double dist = currentPos.distanceTo(lastPos);
        // P0 #5: 传送检测改为合理阈值，防止频繁传送绕过燃料消耗
        // 10格过于宽松，改为检测维度变化或超过模拟距离
        if (dist > 128.0) {  // 8个区块的距离，超过此值视为传送
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

        if (fuel.isEmpty()) {
            TransformationManager.setTransformed(core, false);
            TransformationManager.removeTransformationAttributes(player);
            TransformationManager.removeOverweightPenalty(player);
            player.removeEffect(ModMobEffects.FLAMMABLE);
            PlayerAircraftHelper.recallAircraftForPlayer(player);
            lastWeaponLoad.remove(uuid);
            accumulatedDistance.remove(uuid);
            lastPlayerPos.remove(uuid);
            player.displayClientMessage(
                    Component.translatable("message.piranport.fuel_depleted"), true);
            return;
        }

        accumulatedDistance.put(uuid, acc);
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
        ShipCoreItem.tryAutoLaunchFighter(player.level(), player, coreStack, coreSlot);
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
            ShipCoreItem.tryAutoFireAntiAirMissile(player.level(), player, coreStack, coreSlot);
        }
    }
}
