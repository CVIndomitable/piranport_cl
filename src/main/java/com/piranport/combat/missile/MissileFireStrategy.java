package com.piranport.combat.missile;

import com.piranport.aviation.FireControlManager;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.SlotCooldowns;
import com.piranport.component.WeaponCooldown;
import com.piranport.combat.TransformationManager;
import com.piranport.debug.PiranPortDebug;
import com.piranport.entity.MissileEntity;
import com.piranport.item.ExperienceShellItem;
import com.piranport.item.MissileLauncherItem;
import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * 导弹开火策略 — 从 {@link com.piranport.item.ShipCoreCombat} 提取的导弹相关静态方法集合。
 *
 * <p><b>职责</b>：处理导弹发射器（反舰/防空/火箭弹）的开火入口、装填模式选择、实体生成和自动瞄准。
 * <p><b>线程模型</b>：服务端主线程。
 * <p><b>设计</b>：纯静态方法集合，无实例状态；方法签名沿用原 {@code ShipCoreCombat} 的入参。
 * <p><b>迁移说明</b>：方法体与原 {@code ShipCoreCombat} 完全一致，仅签名改为 {@code public static} 以便跨包调用。
 */
public class MissileFireStrategy {

    private MissileFireStrategy() {}

    public static void fireMissiles(Level level, Player player, ItemStack coreStack,
                                    Inventory inv, int weaponSlot, int coreSlot,
                                    MissileLauncherItem launcher, SlotCooldowns cooldowns) {
        if (launcher.isManualReload()) {
            // 反舰导弹/火箭弹：仅手动装填（装填设施），不受鱼雷再装填强化影响
            fireMissileManual(level, player, coreStack, inv, weaponSlot, launcher, cooldowns);
        } else {
            // 防空导弹：自动从背包装填
            fireMissileAutoReload(level, player, coreStack, inv, weaponSlot, coreSlot, launcher, cooldowns);
        }
    }

    /** 反舰导弹/火箭弹：消耗 LOADED_AMMO，无冷却，仅装填设施装弹。 */
    public static void fireMissileManual(Level level, Player player, ItemStack coreStack,
                                         Inventory inv, int weaponSlot,
                                         MissileLauncherItem launcher, SlotCooldowns cooldowns) {
        ItemStack launcherStack = weaponSlot == 40 ? inv.offhand.get(0) : inv.items.get(weaponSlot);

        // Creative mode: auto-find missile from inventory
        if (player.getAbilities().instabuild) {
            Item ammoItem = launcher.getAmmoItem();
            int coreSlot = -1;
            int ammoSlot = -1;

            for (int i = 0; i < inv.items.size(); i++) {
                if (i == weaponSlot) continue;
                ItemStack s = inv.items.get(i);
                if (s.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(s)) {
                    coreSlot = i;
                }
                if (ammoSlot == -1 && !s.isEmpty() && s.is(ammoItem)) {
                    ammoSlot = i;
                }
            }
            if (ammoSlot == -1 && weaponSlot != 40 && coreSlot != 40) {
                ItemStack oh = inv.offhand.get(0);
                if (!oh.isEmpty() && oh.is(ammoItem)) {
                    ammoSlot = 40;
                }
            }

            String ammoId;
            if (ammoSlot == -1) {
                // 创造模式：使用默认弹药
                ammoId = BuiltInRegistries.ITEM.getKey(ammoItem).toString();
            } else {
                ammoId = BuiltInRegistries.ITEM.getKey(ammoItem).toString();
            }

            spawnMissile(level, player, launcherStack, launcher, ammoId);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 0.8f);
            return;
        }

        // Survival mode: use LOADED_AMMO component
        LoadedAmmo loaded = launcherStack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (!loaded.hasAmmo()) {
            player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
            return;
        }

        // 发射1枚导弹
        String ammoId = loaded.ammoItemId();
        spawnMissile(level, player, launcherStack, launcher, ammoId);

        // 消耗1枚
        int remaining = loaded.count() - 1;
        if (remaining <= 0) {
            launcherStack.remove(ModDataComponents.LOADED_AMMO.get());
        } else {
            launcherStack.set(ModDataComponents.LOADED_AMMO.get(), new LoadedAmmo(remaining, ammoId));
        }

        // 无冷却 — 反舰/火箭可连续发射


        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    /** 导弹自动装填：从背包消耗弹药，发射后进入冷却。防空导弹专用。 */
    public static void fireMissileAutoReload(Level level, Player player, ItemStack coreStack,
                                               Inventory inv, int weaponSlot, int coreSlot,
                                               MissileLauncherItem launcher, SlotCooldowns cooldowns) {
        Item ammoItem = launcher.getAmmoItem();

        // 查找弹药
        int ammoSlot = -1;
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == coreSlot || i == weaponSlot) continue;
            ItemStack s = inv.items.get(i);
            if (!s.isEmpty() && s.is(ammoItem)) {
                ammoSlot = i;
                break;
            }
        }
        if (ammoSlot == -1 && weaponSlot != 40 && coreSlot != 40) {
            ItemStack oh = inv.offhand.get(0);
            if (!oh.isEmpty() && oh.is(ammoItem)) {
                ammoSlot = 40;
            }
        }

        // 创造模式：如果没有弹药，使用默认弹药ID
        String ammoId;
        if (ammoSlot == -1) {
            if (!player.getAbilities().instabuild) {
                player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
                return;
            }
            // 创造模式：使用默认弹药
            ammoId = BuiltInRegistries.ITEM.getKey(ammoItem).toString();
        } else {
            // 有弹药：使用物品栏中的弹药类型
            ammoId = BuiltInRegistries.ITEM.getKey(ammoItem).toString();
            // 创造模式：不消耗弹药
            if (!player.getAbilities().instabuild) {
                PiranPortDebug.consumeAmmo(
                        ammoSlot == 40 ? inv.offhand.get(0) : inv.items.get(ammoSlot), 1);
            }
        }

        // 发射
        ItemStack launcherStack = weaponSlot == 40 ? inv.offhand.get(0) : inv.items.get(weaponSlot);
        spawnMissile(level, player, launcherStack, launcher, ammoId);

        // 检查剩余弹药（避免冷却后才发现无弹药）
        int nextAvailable = 0;
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == coreSlot || i == weaponSlot) continue;
            ItemStack s = inv.items.get(i);
            if (!s.isEmpty() && s.is(ammoItem)) {
                nextAvailable += s.getCount();
            }
        }
        if (weaponSlot != 40 && coreSlot != 40) {
            ItemStack oh = inv.offhand.get(0);
            if (!oh.isEmpty() && oh.is(ammoItem)) {
                nextAvailable += oh.getCount();
            }
        }

        // Creative mode: always has next round
        if (player.getAbilities().instabuild) {
            nextAvailable = 1;
        }

        // 应用冷却
        if (nextAvailable > 0) {
            int cd = TransformationManager.boostedCooldown(player,
                    ExperienceShellItem.applyCooldownReduction(launcherStack, launcher.getCooldownTicks()));
            coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                    cooldowns.withSlotCooldown(weaponSlot, cd, level.getGameTime()));
            launcherStack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                    WeaponCooldown.of(level.getGameTime(), cd));
        } else {
            int penaltyTicks = 10;
            coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                    cooldowns.withSlotCooldown(weaponSlot, penaltyTicks, level.getGameTime()));
            launcherStack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                    WeaponCooldown.of(level.getGameTime(), penaltyTicks));
            player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
        }


        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    /** 生成导弹实体：玩家前方0.5格处，沿视线方向发射。 */
    public static void spawnMissile(Level level, Player player, ItemStack launcherStack,
                                    MissileLauncherItem launcher, String displayItemId) {
        spawnMissileWithDir(level, player, launcherStack, launcher, displayItemId, player.getLookAngle());
    }

    /**
     * 通用导弹生成：在玩家眼睛 + dir*0.5 处生成导弹，以 dir 方向按 initialSpeed 射出。
     * dir 预期为单位向量；非单位向量将被规整化。
     */
    public static void spawnMissileWithDir(Level level, Player player, ItemStack launcherStack,
                                             MissileLauncherItem launcher, String displayItemId,
                                             Vec3 dir) {
        Vec3 d = dir.lengthSqr() > 1e-6 ? dir.normalize() : player.getLookAngle();
        MissileEntity missile = new MissileEntity(level, launcher.getMissileType(),
                ExperienceShellItem.applyDamageBonus(launcherStack, launcher.getDamage()),
                launcher.getArmorPen(),
                ExperienceShellItem.applyExplosionBonus(launcherStack, launcher.getExplosionPower()),
                displayItemId);
        missile.setOwner(player);
        // Y 跟随 dir.y 偏移，避免抬头/俯冲时导弹从胸前喷出
        missile.setPos(
                player.getX() + d.x * 0.5,
                player.getEyeY() - 0.1 + d.y * 0.5,
                player.getZ() + d.z * 0.5);
        float initSpeed = launcher.getMissileType().initialSpeed;
        missile.setDeltaMovement(d.x * initSpeed, d.y * initSpeed, d.z * initSpeed);
        level.addFreshEntity(missile);
    }

    /**
     * Auto-fire anti-air missiles from the player's hotbar when auto-launch is active.
     * Scans hotbar for ANTI_AIR MissileLauncherItems, checks cooldown and ammo, fires one missile.
     * The missile is aimed toward the fire control target (or upward if no lock).
     */
    public static boolean tryAutoFireAntiAirMissile(Level level, Player player, ItemStack coreStack, int coreSlot) {
        if (!(coreStack.getItem() instanceof ShipCoreItem)) return false;
        if (level.isClientSide()) return false;

        Inventory inv = player.getInventory();
        SlotCooldowns cooldowns = coreStack.getOrDefault(
                ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
        long gameTime = level.getGameTime();

        // Scan hotbar (slots 0-8) for anti-air missile launchers
        for (int slot = 0; slot < 9; slot++) {
            if (slot == coreSlot) continue;
            ItemStack stack = inv.items.get(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof MissileLauncherItem launcher)) continue;
            if (launcher.getMissileType() != MissileEntity.MissileType.ANTI_AIR) continue;

            // Check cooldown
            if (cooldowns.isOnCooldown(slot, gameTime)) continue;

            // Find ammo in inventory
            Item ammoItem = launcher.getAmmoItem();
            int ammoSlot = -1;
            for (int i = 0; i < inv.items.size(); i++) {
                if (i == coreSlot || i == slot) continue;
                ItemStack s = inv.items.get(i);
                if (!s.isEmpty() && s.is(ammoItem)) {
                    ammoSlot = i;
                    break;
                }
            }
            if (ammoSlot == -1 && slot != 40 && coreSlot != 40) {
                ItemStack oh = inv.offhand.get(0);
                if (!oh.isEmpty() && oh.is(ammoItem)) {
                    ammoSlot = 40;
                }
            }
            if (ammoSlot == -1) continue; // No ammo for this launcher, try next

            // Consume 1 ammo
            String ammoId = BuiltInRegistries.ITEM.getKey(ammoItem).toString();
            PiranPortDebug.consumeAmmo(
                    ammoSlot == 40 ? inv.offhand.get(0) : inv.items.get(ammoSlot), 1);

            // Spawn missile aimed at fire control target
            spawnMissileAutoAim(level, player, stack, launcher, ammoId);

            // Apply cooldown
            int cd = TransformationManager.boostedCooldown(player,
                    ExperienceShellItem.applyCooldownReduction(stack, launcher.getCooldownTicks()));
            coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                    cooldowns.withSlotCooldown(slot, cd, gameTime));
            stack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                    WeaponCooldown.of(gameTime, cd));

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 0.8f);
            return true;
        }
        return false;
    }

    /** Spawn a missile aimed toward the fire control target, nearest hostile, or upward. */
    public static void spawnMissileAutoAim(Level level, Player player, ItemStack launcherStack,
                                             MissileLauncherItem launcher, String displayItemId) {
        Vec3 aimDir = null;
        if (level instanceof ServerLevel sl) {
            // 1. Fire control target
            List<UUID> fcTargets = FireControlManager.getTargets(player.getUUID());
            for (UUID targetUUID : fcTargets) {
                net.minecraft.world.entity.Entity target = sl.getEntity(targetUUID);
                if (target != null && target.isAlive() && !target.isUnderWater()
                        && !(target instanceof net.minecraft.world.Container)) {
                    Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0)
                            .subtract(player.getEyePosition());
                    if (toTarget.lengthSqr() > 0.01) {
                        aimDir = toTarget.normalize();
                    }
                    break;
                }
            }
            // 2. No fire control lock — find nearest hostile mob (Enemy interface covers Phantom/Vex/Monster)
            //    防空导弹自动瞄准仅限飞行目标（离地至少2格的空中目标）
            final boolean antiAirOnly = launcher.getMissileType() == MissileEntity.MissileType.ANTI_AIR;
            if (aimDir == null) {
                LivingEntity nearest = null;
                double bestDist = 32.0;
                for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(32.0),
                        e -> e.isAlive() && e.isPickable() && e instanceof Enemy && !e.isUnderWater())) {
                    if (antiAirOnly) {
                        // Anti-air missiles only target airborne enemies (at least 2 blocks above ground)
                        net.minecraft.core.BlockPos below = mob.blockPosition().below(2);
                        if (mob.onGround() || level.getBlockState(below).isSolid()) {
                            continue;
                        }
                    }
                    double d = player.distanceTo(mob);
                    if (d < bestDist) {
                        bestDist = d;
                        nearest = mob;
                    }
                }
                if (nearest != null) {
                    Vec3 toTarget = nearest.position().add(0, nearest.getBbHeight() * 0.5, 0)
                            .subtract(player.getEyePosition());
                    if (toTarget.lengthSqr() > 0.01) {
                        aimDir = toTarget.normalize();
                    }
                }
            }
        }
        // 3. Fallback: upward launch (avoid hitting ground)
        if (aimDir == null) {
            Vec3 look = player.getLookAngle();
            aimDir = new Vec3(look.x, Math.max(look.y, 0.5), look.z).normalize();
        }

        spawnMissileWithDir(level, player, launcherStack, launcher, displayItemId, aimDir);
    }
}