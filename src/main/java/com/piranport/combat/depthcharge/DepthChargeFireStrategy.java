package com.piranport.combat.depthcharge;

import com.piranport.combat.TransformationManager;
import com.piranport.combat.util.CombatFireUtils;
import com.piranport.config.ModProjectilesConfig;
import com.piranport.component.SlotCooldowns;
import com.piranport.component.WeaponCooldown;
import com.piranport.component.LoadedAmmo;
import com.piranport.debug.PiranPortDebug;
import com.piranport.entity.DepthChargeEntity;
import com.piranport.item.DepthChargeLauncherItem;
import com.piranport.item.ExperienceShellItem;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 深水炸弹（深弹）发射策略 — 从 {@code ShipCoreCombat} 提取的 {@code fireDepthCharges}
 * 与 {@code spawnDepthCharge} 静态方法。
 *
 * <p>职责：处理舰装核心深弹发射器的弹药检查、消耗、散布发射、发射器损耗与冷却写入。
 * <p><b>线程模型</b>：服务端主线程。
 * <p><b>设计</b>：纯静态方法集合，无实例状态；调用方仍可继续使用原 {@code ShipCoreCombat.fireDepthCharges}。
 */
public final class DepthChargeFireStrategy {

    private DepthChargeFireStrategy() {}

    public static void fireDepthCharges(Level level, Player player, ItemStack coreStack,
                                        Inventory inv, int weaponSlot, int coreSlot,
                                        DepthChargeLauncherItem launcher, SlotCooldowns cooldowns) {
        ItemStack launcherStack = weaponSlot == 40 ? inv.offhand.get(0) : inv.items.get(weaponSlot);
        int chargeCount = launcher.getChargeCount();
        int cooldown = ExperienceShellItem.applyCooldownReduction(player, launcher.getCooldownTicks());
        // 基础伤害/威力与 DepthChargeEntity 兜底值同源（global.depth_charge.*），再叠加玩家强化加成。
        float damage = ExperienceShellItem.applyDamageBonus(player,
                (float) (double) ModProjectilesConfig.DEPTH_CHARGE_DAMAGE.get());
        float explosionPower = ExperienceShellItem.applyExplosionBonus(player,
                (float) (double) ModProjectilesConfig.DEPTH_CHARGE_EXPLOSION_POWER.get());

        LoadedAmmo loaded = launcherStack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (!player.getAbilities().instabuild && (!loaded.hasAmmo() || loaded.count() < chargeCount)) {
            player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
            return;
        }
        if (loaded.hasAmmo()) launcherStack.remove(ModDataComponents.LOADED_AMMO.get());

        // Spawn depth charges based on spread pattern（各弹初速/偏角走终端 global.depth_charge.*）
        Vec3 look = player.getLookAngle();
        Vec3 horizLook = new Vec3(look.x, 0, look.z).normalize();
        double speedSingle = ModProjectilesConfig.DEPTH_CHARGE_SPEED_SINGLE.get();
        double speedFar = ModProjectilesConfig.DEPTH_CHARGE_SPEED_FAR.get();
        double speedNear = ModProjectilesConfig.DEPTH_CHARGE_SPEED_NEAR.get();
        double speedTriangle = ModProjectilesConfig.DEPTH_CHARGE_SPEED_TRIANGLE.get();
        double spreadRad = Math.toRadians(ModProjectilesConfig.DEPTH_CHARGE_SPREAD_DEG.get());
        switch (launcher.getSpreadPattern()) {
            case SINGLE -> {
                spawnDepthCharge(level, player, horizLook, 0.0, speedSingle, damage, explosionPower);
            }
            case FRONT_BACK -> {
                spawnDepthCharge(level, player, horizLook, 0.0, speedFar, damage, explosionPower);   // far
                spawnDepthCharge(level, player, horizLook, 0.0, speedNear, damage, explosionPower);   // near
            }
            case TRIANGLE -> {
                spawnDepthCharge(level, player, horizLook, 0.0, speedFar, damage, explosionPower);   // center far
                Vec3 left = CombatFireUtils.rotateHorizontal(horizLook, -spreadRad);
                spawnDepthCharge(level, player, left, 0.0, speedTriangle, damage, explosionPower);
                Vec3 right = CombatFireUtils.rotateHorizontal(horizLook, spreadRad);
                spawnDepthCharge(level, player, right, 0.0, speedTriangle, damage, explosionPower);
            }
        }

        // Damage launcher
        boolean launcherBroken = false;
        if (!launcherStack.isEmpty()) {
            int newDamage = launcherStack.getDamageValue() + 1;
            if (newDamage >= launcherStack.getMaxDamage()) {
                if (weaponSlot == 40) inv.offhand.set(0, ItemStack.EMPTY);
                else inv.items.set(weaponSlot, ItemStack.EMPTY);
                launcherBroken = true;
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f + level.random.nextFloat() * 0.4f);
            } else {
                launcherStack.setDamageValue(newDamage);
            }
        }

        if (!launcherBroken) {
            int boostedCooldown = TransformationManager.boostedCooldown(player, cooldown);
            coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                    cooldowns.withSlotCooldown(player.getUUID(), weaponSlot, boostedCooldown, level.getGameTime()));
            launcherStack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                    WeaponCooldown.of(player.getUUID(), level.getGameTime(), boostedCooldown));
        }


        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 0.6f);
    }

    public static void spawnDepthCharge(Level level, Player player, Vec3 dir, double angleOffset,
                                        double speed, float damage, float explosionPower) {
        DepthChargeEntity dc = new DepthChargeEntity(level, player, damage, explosionPower);
        dc.setPos(player.getX() + dir.x * 0.5, player.getEyeY() - 0.3, player.getZ() + dir.z * 0.5);
        dc.setDeltaMovement(dir.x * speed, 0.3, dir.z * speed);
        level.addFreshEntity(dc);
    }
}
