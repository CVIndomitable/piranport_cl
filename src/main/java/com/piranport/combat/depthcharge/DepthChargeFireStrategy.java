package com.piranport.combat.depthcharge;

import com.piranport.combat.TransformationManager;
import com.piranport.combat.util.CombatFireUtils;
import com.piranport.component.SlotCooldowns;
import com.piranport.component.WeaponCooldown;
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
        int cooldown = ExperienceShellItem.applyCooldownReduction(launcherStack, launcher.getCooldownTicks());
        float damage = ExperienceShellItem.applyDamageBonus(launcherStack, 14f);
        float explosionPower = ExperienceShellItem.applyExplosionBonus(launcherStack, 3.0f);

        // Creative mode: skip ammo check and consumption
        if (!player.getAbilities().instabuild) {
            // Count available depth charge ammo in inventory
            int available = 0;
            for (int i = 0; i < inv.items.size(); i++) {
                if (i == coreSlot || i == weaponSlot) continue;
                ItemStack s = inv.items.get(i);
                if (!s.isEmpty() && s.is(ModItems.DEPTH_CHARGE.get())) {
                    available += s.getCount();
                }
            }
            if (weaponSlot != 40 && coreSlot != 40) {
                ItemStack oh = inv.offhand.get(0);
                if (!oh.isEmpty() && oh.is(ModItems.DEPTH_CHARGE.get())) {
                    available += oh.getCount();
                }
            }

            if (available < chargeCount) {
                player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
                return;
            }

            // Consume ammo
            int toConsume = chargeCount;
            for (int i = 0; i < inv.items.size() && toConsume > 0; i++) {
                if (i == coreSlot || i == weaponSlot) continue;
                ItemStack s = inv.items.get(i);
                if (!s.isEmpty() && s.is(ModItems.DEPTH_CHARGE.get())) {
                    int take = Math.min(toConsume, s.getCount());
                    PiranPortDebug.consumeAmmo(s, take);
                    toConsume -= take;
                }
            }
            if (toConsume > 0 && weaponSlot != 40 && coreSlot != 40) {
                ItemStack oh = inv.offhand.get(0);
                if (!oh.isEmpty() && oh.is(ModItems.DEPTH_CHARGE.get())) {
                    int take = Math.min(toConsume, oh.getCount());
                    PiranPortDebug.consumeAmmo(oh, take);
                    toConsume -= take;
                }
            }
        } else {
            // 创造模式：即使没有深弹也允许发射（使用默认深弹）
            // 无需额外检查，直接发射
        }

        // Spawn depth charges based on spread pattern
        Vec3 look = player.getLookAngle();
        Vec3 horizLook = new Vec3(look.x, 0, look.z).normalize();
        switch (launcher.getSpreadPattern()) {
            case SINGLE -> {
                spawnDepthCharge(level, player, horizLook, 0.0, 0.6, damage, explosionPower);
            }
            case FRONT_BACK -> {
                spawnDepthCharge(level, player, horizLook, 0.0, 0.7, damage, explosionPower);   // far
                spawnDepthCharge(level, player, horizLook, 0.0, 0.4, damage, explosionPower);   // near
            }
            case TRIANGLE -> {
                spawnDepthCharge(level, player, horizLook, 0.0, 0.7, damage, explosionPower);   // center far
                Vec3 left = CombatFireUtils.rotateHorizontal(horizLook, Math.toRadians(-20));
                spawnDepthCharge(level, player, left, 0.0, 0.5, damage, explosionPower);
                Vec3 right = CombatFireUtils.rotateHorizontal(horizLook, Math.toRadians(20));
                spawnDepthCharge(level, player, right, 0.0, 0.5, damage, explosionPower);
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
                    cooldowns.withSlotCooldown(weaponSlot, boostedCooldown, level.getGameTime()));
            launcherStack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                    WeaponCooldown.of(level.getGameTime(), boostedCooldown));
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
