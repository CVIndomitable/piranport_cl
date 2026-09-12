package com.piranport.combat.torpedo;

import com.piranport.combat.TorpedoGuidanceManager;
import com.piranport.combat.TransformationManager;
import com.piranport.combat.util.CombatFireUtils;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.SlotCooldowns;
import com.piranport.component.WeaponCooldown;
import com.piranport.debug.PiranPortDebug;
import com.piranport.entity.TorpedoEntity;
import com.piranport.item.ExperienceShellItem;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.TorpedoItem;
import com.piranport.item.TorpedoLauncherItem;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 鱼雷开火策略 — 从 {@code ShipCoreCombat} 提取的鱼雷发射相关静态方法集合。
 *
 * <p>包含手动模式开火、背包模式开火、以及创造模式默认鱼雷类型解析。
 *
 * <p><b>线程模型</b>：服务端主线程。
 * <p><b>设计</b>：纯静态方法集合，无实例状态。
 *
 * @see com.piranport.item.ShipCoreCombat
 */
public class TorpedoFireStrategy {

    private TorpedoFireStrategy() {}

    public static void fireTorpedosManualMode(Level level, Player player, ItemStack coreStack,
            Inventory inv, int weaponSlot, TorpedoLauncherItem launcher, SlotCooldowns cooldowns) {
        ItemStack launcherStack = weaponSlot == 40 ? inv.offhand.get(0) : inv.items.get(weaponSlot);
        int tubeCount = launcher.getTubeCount();
        int caliber = launcher.getCaliber();

        // Creative mode: auto-find torpedo from inventory
        if (player.getAbilities().instabuild) {
            TorpedoItem torpedoType = null;
            int coreSlot = -1;
            for (int i = 0; i < inv.items.size(); i++) {
                if (i == weaponSlot) continue;
                ItemStack s = inv.items.get(i);
                if (s.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(s)) {
                    coreSlot = i;
                }
                if (torpedoType == null && s.getItem() instanceof TorpedoItem ti && ti.getCaliber() == caliber) {
                    torpedoType = ti;
                }
            }
            if (torpedoType == null && weaponSlot != 40 && coreSlot != 40) {
                ItemStack oh = inv.offhand.get(0);
                if (oh.getItem() instanceof TorpedoItem ti && ti.getCaliber() == caliber) {
                    torpedoType = ti;
                }
            }

            if (torpedoType == null) {
                player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
                return;
            }

            String ammoId = BuiltInRegistries.ITEM.getKey(torpedoType).toString();
            boolean magnetic = CombatFireUtils.isMagneticTorpedo(ammoId);
            boolean wireGuided = CombatFireUtils.isWireGuidedTorpedo(ammoId);
            boolean acousticHoming = CombatFireUtils.isAcousticTorpedo(ammoId);
            float torpedoSpeed = torpedoType.getSpeed();
            float[] angles = CombatFireUtils.getSpreadAngles(tubeCount);
            Vec3 look = player.getLookAngle();

            TorpedoEntity primaryGuided = null;
            for (float angle : angles) {
                Vec3 dir = CombatFireUtils.rotateHorizontal(look, Math.toRadians(angle));
                TorpedoEntity torpedo = new TorpedoEntity(level, player, caliber);
                torpedo.setDamage(ExperienceShellItem.applyDamageBonus(launcherStack, torpedoType.getDamage()));
                torpedo.setSpeed(torpedoType.getSpeed());
                torpedo.setLifetime(torpedoType.getLifetimeTicks());
                if (magnetic) torpedo.setMagnetic(true);
                if (wireGuided) torpedo.setWireGuided(true);
                if (acousticHoming) torpedo.setAcoustic(true);
                torpedo.setPos(player.getX() + dir.x * 0.5, player.getEyeY() - 0.3, player.getZ() + dir.z * 0.5);
                torpedo.setDeltaMovement(dir.x * torpedoSpeed, 0, dir.z * torpedoSpeed);
                level.addFreshEntity(torpedo);
                if (wireGuided && primaryGuided == null) primaryGuided = torpedo;
            }
            if (primaryGuided != null && player instanceof ServerPlayer sp) {
                TorpedoGuidanceManager.startGuidance(sp, primaryGuided);
            }

            int cooldown = ExperienceShellItem.applyCooldownReduction(launcherStack, launcher.getCooldownTicks());
            int boostedCooldown = TransformationManager.boostedCooldown(player, cooldown);
            coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                    cooldowns.withSlotCooldown(weaponSlot, boostedCooldown, level.getGameTime()));
            launcherStack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                    WeaponCooldown.of(level.getGameTime(), boostedCooldown));

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4f, 0.4f);
            return;
        }

        // Survival mode: use LOADED_AMMO component
        LoadedAmmo loaded = launcherStack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (!loaded.hasAmmo() || loaded.count() < tubeCount) {
            player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
            return;
        }

        // Use caliber from method start
        int cooldown = ExperienceShellItem.applyCooldownReduction(launcherStack, launcher.getCooldownTicks());
        boolean magnetic = CombatFireUtils.isMagneticTorpedo(loaded.ammoItemId());
        boolean wireGuided = CombatFireUtils.isWireGuidedTorpedo(loaded.ammoItemId());
        boolean acousticHoming = CombatFireUtils.isAcousticTorpedo(loaded.ammoItemId());
        // Resolve torpedo item to read per-item stats
        Item loadedItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(loaded.ammoItemId()));
        TorpedoItem loadedTorpedo = loadedItem instanceof TorpedoItem ti ? ti : null;
        float torpedoSpeed = loadedTorpedo != null ? loadedTorpedo.getSpeed() : (caliber == 610 ? 1.0f : 1.2f);
        float[] angles = CombatFireUtils.getSpreadAngles(tubeCount);
        Vec3 look = player.getLookAngle();

        TorpedoEntity primaryGuided = null;
        for (float angle : angles) {
            Vec3 dir = CombatFireUtils.rotateHorizontal(look, Math.toRadians(angle));
            TorpedoEntity torpedo = new TorpedoEntity(level, player, caliber);
            if (loadedTorpedo != null) {
                torpedo.setDamage(ExperienceShellItem.applyDamageBonus(launcherStack, loadedTorpedo.getDamage()));
                torpedo.setSpeed(loadedTorpedo.getSpeed());
                torpedo.setLifetime(loadedTorpedo.getLifetimeTicks());
            }
            if (magnetic) torpedo.setMagnetic(true);
            if (wireGuided) torpedo.setWireGuided(true);
            if (acousticHoming) torpedo.setAcoustic(true);
            torpedo.setPos(player.getX() + dir.x * 0.5, player.getEyeY() - 0.3, player.getZ() + dir.z * 0.5);
            torpedo.setDeltaMovement(dir.x * torpedoSpeed, 0, dir.z * torpedoSpeed);
            level.addFreshEntity(torpedo);
            if (wireGuided && primaryGuided == null) primaryGuided = torpedo;
        }
        if (primaryGuided != null && player instanceof ServerPlayer sp) {
            TorpedoGuidanceManager.startGuidance(sp, primaryGuided);
        }

        // Consume all loaded torpedoes
        launcherStack.remove(ModDataComponents.LOADED_AMMO.get());

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
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4f, 0.4f);
    }

    public static void fireTorpedosInventoryMode(Level level, Player player, ItemStack coreStack,
                                            Inventory inv, int weaponSlot, int coreSlot,
                                            TorpedoLauncherItem launcher, SlotCooldowns cooldowns) {
        ItemStack launcherStack = weaponSlot == 40 ? inv.offhand.get(0) : inv.items.get(weaponSlot);
        LoadedAmmo loaded = launcherStack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);

        // 优先使用已装填弹药（装填设施装的），用完后才自动从背包装填
        if (loaded.hasAmmo() && loaded.count() >= launcher.getTubeCount()) {
            fireTorpedosManualMode(level, player, coreStack, inv, weaponSlot, launcher, cooldowns);
            return;
        }

        int caliber = launcher.getCaliber();
        int tubeCount = launcher.getTubeCount();
        int cooldown = ExperienceShellItem.applyCooldownReduction(launcherStack, launcher.getCooldownTicks());

        // Find first matching torpedo to determine type (strict: only consume same item type)
        TorpedoItem torpedoType = null;
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == coreSlot || i == weaponSlot) continue;
            ItemStack s = inv.items.get(i);
            if (!s.isEmpty() && s.getItem() instanceof TorpedoItem ti && ti.getCaliber() == caliber) {
                torpedoType = ti;
                break;
            }
        }
        if (torpedoType == null && weaponSlot != 40 && coreSlot != 40) {
            ItemStack oh = inv.offhand.get(0);
            if (!oh.isEmpty() && oh.getItem() instanceof TorpedoItem ti && ti.getCaliber() == caliber) {
                torpedoType = ti;
            }
        }

        // 创造模式：如果没有鱼雷，使用默认鱼雷类型
        if (torpedoType == null) {
            if (!player.getAbilities().instabuild) {
                player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
                return;
            }
            // 创造模式：使用默认鱼雷
            torpedoType = getDefaultTorpedoForCaliber(caliber);
            if (torpedoType == null) {
                player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
                return;
            }
        }

        // Creative mode: skip ammo consumption
        if (!player.getAbilities().instabuild) {
            // Count available ammo of the same type
            int available = 0;
            for (int i = 0; i < inv.items.size(); i++) {
                if (i == coreSlot || i == weaponSlot) continue;
                ItemStack s = inv.items.get(i);
                if (!s.isEmpty() && s.getItem() == torpedoType) {
                    available += s.getCount();
                }
            }
            if (weaponSlot != 40 && coreSlot != 40) {
                ItemStack oh = inv.offhand.get(0);
                if (!oh.isEmpty() && oh.getItem() == torpedoType) {
                    available += oh.getCount();
                }
            }

            if (available < tubeCount) {
                player.displayClientMessage(Component.translatable("message.piranport.insufficient_same_ammo"), true);
                return;
            }

            // Consume ammo before spawning entities (prevent TOCTOU)
            int toConsume = tubeCount;
            for (int i = 0; i < inv.items.size() && toConsume > 0; i++) {
                if (i == coreSlot || i == weaponSlot) continue;
                ItemStack s = inv.items.get(i);
                if (!s.isEmpty() && s.getItem() == torpedoType) {
                    int take = Math.min(toConsume, s.getCount());
                    PiranPortDebug.consumeAmmo(s, take);
                    toConsume -= take;
                }
            }
            if (toConsume > 0 && weaponSlot != 40 && coreSlot != 40) {
                ItemStack oh = inv.offhand.get(0);
                if (!oh.isEmpty() && oh.getItem() == torpedoType) {
                    int take = Math.min(toConsume, oh.getCount());
                    PiranPortDebug.consumeAmmo(oh, take);
                    toConsume -= take;
                }
            }
        }

        boolean magnetic = torpedoType.isMagnetic();
        boolean acousticHoming = torpedoType.isAcoustic();
        boolean wireGuided = torpedoType.isWireGuided();
        boolean oxygen = torpedoType.isOxygen();
        float torpedoSpeed = torpedoType.getSpeed();
        float[] angles = CombatFireUtils.getSpreadAngles(tubeCount);
        Vec3 look = player.getLookAngle();

        TorpedoEntity primaryGuided = null;
        for (float angle : angles) {
            Vec3 dir = CombatFireUtils.rotateHorizontal(look, Math.toRadians(angle));
            TorpedoEntity torpedo = new TorpedoEntity(level, player, caliber);
            torpedo.setDamage(ExperienceShellItem.applyDamageBonus(launcherStack, torpedoType.getDamage()));
            torpedo.setSpeed(torpedoType.getSpeed());
            torpedo.setLifetime(torpedoType.getLifetimeTicks());
            if (magnetic) torpedo.setMagnetic(true);
            if (acousticHoming) torpedo.setAcoustic(true);
            if (oxygen) torpedo.setOxygen(true);
            if (wireGuided) torpedo.setWireGuided(true);
            torpedo.setPos(player.getX() + dir.x * 0.5, player.getEyeY() - 0.3, player.getZ() + dir.z * 0.5);
            torpedo.setDeltaMovement(dir.x * torpedoSpeed, 0, dir.z * torpedoSpeed);
            level.addFreshEntity(torpedo);
            if (wireGuided && primaryGuided == null) primaryGuided = torpedo;
        }
        if (primaryGuided != null && player instanceof ServerPlayer sp) {
            TorpedoGuidanceManager.startGuidance(sp, primaryGuided);
        }

        // Damage launcher in-inventory
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

        // Check if enough torpedoes remain for next salvo
        int nextAvailable = 0;
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == coreSlot || i == weaponSlot) continue;
            ItemStack s = inv.items.get(i);
            if (!s.isEmpty() && s.getItem() instanceof TorpedoItem ti && ti.getCaliber() == caliber) {
                nextAvailable += s.getCount();
            }
        }
        if (weaponSlot != 40 && coreSlot != 40) {
            ItemStack oh = inv.offhand.get(0);
            if (!oh.isEmpty() && oh.getItem() instanceof TorpedoItem ti && ti.getCaliber() == caliber) {
                nextAvailable += oh.getCount();
            }
        }

        if (!launcherBroken) {
            if (nextAvailable >= tubeCount) {
                int boostedCooldown = TransformationManager.boostedCooldown(player, cooldown);
                coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                        cooldowns.withSlotCooldown(weaponSlot, boostedCooldown, level.getGameTime()));
                launcherStack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                        WeaponCooldown.of(level.getGameTime(), boostedCooldown));
            } else {
                // 背包弹药不足，设置短冷却提示玩家需要补充弹药
                int penaltyTicks = 10;
                coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                        cooldowns.withSlotCooldown(weaponSlot, penaltyTicks, level.getGameTime()));
                launcherStack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                        WeaponCooldown.of(level.getGameTime(), penaltyTicks));
            }
        }


        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4f, 0.4f);
    }

    /**
     * 获取指定口径的默认鱼雷类型（创造模式使用）
     */
    public static TorpedoItem getDefaultTorpedoForCaliber(int caliber) {
        return switch (caliber) {
            case 533 -> (TorpedoItem) ModItems.TORPEDO_533MM.get();
            case 610 -> (TorpedoItem) ModItems.TORPEDO_610MM.get();
            default -> null;
        };
    }
}