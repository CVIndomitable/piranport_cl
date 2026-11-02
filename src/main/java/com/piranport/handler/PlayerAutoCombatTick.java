package com.piranport.handler;

import com.piranport.aviation.FireControlManager;
import com.piranport.combat.AASilenceManager;
import com.piranport.combat.AutoModeState;
import com.piranport.combat.CombatTargeting;
import com.piranport.combat.TransformationManager;
import com.piranport.entity.AircraftEntity;
import com.piranport.item.AutoCIWSItem;
import com.piranport.item.ShipCoreCombat;
import com.piranport.item.ShipCoreItem;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 战斗机、防空导弹及近防炮调度，仅在服务端主线程执行。
 *
 * <p>战斗机/导弹与近防炮保持两个入口：前者可能刷新防空静默窗口，
 * 总入口必须在两者之间衰减静默窗口，不能把近防炮并入40 tick的扫描周期。
 */
final class PlayerAutoCombatTick {
    private PlayerAutoCombatTick() {}

    private static final int AUTO_COMBAT_INTERVAL = 40;

    /** 战斗机自动升空 + 防空导弹（每40tick，错峰执行） */
    static void tickAutoCombatIfNeeded(Player player) {
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

    /** 自动发射战斗机锁定附近高于玩家的飞行敌对生物 / 敌机 */
    private static void tickAutoLaunchFighters(Player player, ItemStack coreStack, int coreSlot) {
        // P2-8: PERF 埋点 — 火控解算（每 tick 遍历目标）
        boolean perfEnabled = com.piranport.debug.PiranPortDebug.isServerEnabled();
        long t0 = perfEnabled ? System.nanoTime() : 0L;

        // 扫描类型放宽到 Entity：深海自主飞机 AircraftEntity 不是 LivingEntity，也不是 Enemy。
        // 敌对口径统一走 CombatTargeting.isHostileTarget（玩家飞机按 canHarmPlayer、自主深海飞机按 isAutonomous），
        // 原版空中敌怪仍保留 FlyingMob/Phantom/Vex 的筛选。
        // 只要高于玩家的目标：玩家脚下洞穴/坑里的飞行怪不应触发自动升空，否则战斗机会钻地追目标；
        // 低于或同高度的目标不自动打，但仍可手动火控锁定。
        List<Entity> flyingHostiles = player.level().getEntitiesOfClass(
                Entity.class,
                player.getBoundingBox().inflate(64.0),
                e -> e.isAlive()
                        && e.getY() > player.getY()
                        && CombatTargeting.isHostileTarget(player, e)
                        // 与战斗机攻击目标共用同一口径：FlyingMob（幻翼/恶魂）、Vex、
                        // FlyingAnimal（女仆野生妖精等）和飞行导航怪都算空中目标。
                        && com.piranport.aviation.combat.TargetResolution.isAirborneTarget(e));

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
                Entity nearest = flyingHostiles.stream()
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

    /** 防空导弹：检测32格内高于玩家的空中敌对目标 */
    private static void tickAntiAirMissiles(Player player, ItemStack coreStack, int coreSlot) {
        if (AASilenceManager.isSilenced(player)) return;
        boolean hasAirborneHostile = !player.level().getEntitiesOfClass(
                Entity.class,
                player.getBoundingBox().inflate(32.0),
                e -> {
                    // 只打高于玩家的空中目标：脚下洞穴里的飞行怪不应触发防空，
                    // 否则导弹/战斗机会钻进地下；低于或同高度的目标仍可手动火控锁定。
                    if (!e.isAlive() || !e.isPickable()
                            || e.getY() <= player.getY()
                            || !CombatTargeting.isHostileTarget(player, e)) return false;
                    // 飞机恒为空中目标：AircraftEntity 不继承 LivingEntity，onGround 不是可靠判据
                    if (e instanceof AircraftEntity) return true;
                    if (!(e instanceof Enemy) || e.isUnderWater()) return false;
                    BlockPos below = e.blockPosition().below(2);
                    return !e.onGround() && !player.level().getBlockState(below).isSolid();
                }).isEmpty();
        if (hasAirborneHostile) {
            ShipCoreCombat.tryAutoFireAntiAirMissile(player.level(), player, coreStack, coreSlot);
        }
    }

    /** 近防炮、导弹和战斗机读取同一个自动模式总开关。 */
    private static boolean isAutoFireEnabled(ItemStack transformedCore) {
        return AutoModeState.fromStack(transformedCore) == AutoModeState.ON;
    }

    /** 每 tick执行，自动模式关闭时仍交给原近防炮逻辑处理。 */
    static void tickCloseDefense(Player player, ItemStack transformedCore) {
        AutoCIWSItem.tickAutoCIWS(player, isAutoFireEnabled(transformedCore));
    }
}
