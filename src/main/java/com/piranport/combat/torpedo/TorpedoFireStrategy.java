package com.piranport.combat.torpedo;

import com.piranport.combat.TorpedoGuidanceManager;
import com.piranport.combat.TransformationManager;
import com.piranport.combat.util.CombatFireUtils;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.SlotCooldowns;
import com.piranport.component.WeaponCooldown;
import com.piranport.entity.TorpedoEntity;
import com.piranport.item.ExperienceShellItem;
import com.piranport.item.TorpedoItem;
import com.piranport.item.TorpedoLauncherItem;
import com.piranport.registry.ModDataComponents;
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
 * <p>2026-10-26 决策：生存与创造统一<b>只认膛内弹药</b>。没有已装填鱼雷时不得发射，
 * 不再按「鱼雷再装填」部件分流到背包取弹；创造模式也不能用免消耗旁路绕过装填状态。
 *
 * <p><b>线程模型</b>：服务端主线程。
 * <p><b>设计</b>：纯静态方法集合，无实例状态。
 *
 * @see com.piranport.item.ShipCoreCombat
 */
public class TorpedoFireStrategy {

    private TorpedoFireStrategy() {}

    /** 是否满足一次完整齐射：膛内必须有弹，且数量覆盖全部鱼雷管。 */
    public static boolean hasFullSalvo(LoadedAmmo loaded, int tubeCount) {
        return loaded != null && loaded.hasAmmo() && loaded.count() >= tubeCount;
    }

    /**
     * 膛内弹齐射。所有模式（含创造）都走这一条路径。
     */
    public static void fireTorpedosManualMode(Level level, Player player, ItemStack coreStack,
            Inventory inv, int weaponSlot, TorpedoLauncherItem launcher, SlotCooldowns cooldowns) {
        ItemStack launcherStack = weaponSlot == 40 ? inv.offhand.get(0) : inv.items.get(weaponSlot);
        int tubeCount = launcher.getTubeCount();
        int caliber = launcher.getCaliber();

        // 装填读条中不得开火：R 只启动读条，到期由服务端结算；
        // 结算前膛内仍是空的，这里先给提示，避免玩家以为还能继续射击。
        if (launcherStack.get(ModDataComponents.PENDING_TORPEDO_RELOAD.get()) != null) {
            player.displayClientMessage(Component.translatable("message.piranport.already_reloading"), true);
            return;
        }

        // 生存与创造一致：只认膛内弹药。没有已装填鱼雷时不得发射，
        // 创造模式也不能用「扫描背包 + 免消耗」绕过装填状态。
        LoadedAmmo loaded = launcherStack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (!hasFullSalvo(loaded, tubeCount)) {
            player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
            return;
        }

        int cooldown = ExperienceShellItem.applyCooldownReduction(player, launcher.getCooldownTicks());
        boolean magnetic = CombatFireUtils.isMagneticTorpedo(loaded.ammoItemId());
        boolean wireGuided = CombatFireUtils.isWireGuidedTorpedo(loaded.ammoItemId());
        boolean acousticHoming = CombatFireUtils.isAcousticTorpedo(loaded.ammoItemId());
        // Resolve torpedo item to read per-item stats
        Item loadedItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(loaded.ammoItemId()));
        TorpedoItem loadedTorpedo = loadedItem instanceof TorpedoItem ti ? ti : null;
        float torpedoSpeed = loadedTorpedo != null ? loadedTorpedo.getSpeed() : 0.3f;
        float[] angles = CombatFireUtils.getSpreadAngles(tubeCount);
        Vec3 look = player.getLookAngle();

        TorpedoEntity primaryGuided = null;
        for (float angle : angles) {
            Vec3 dir = CombatFireUtils.rotateHorizontal(look, Math.toRadians(angle));
            TorpedoEntity torpedo = new TorpedoEntity(level, player, caliber);
            if (loadedTorpedo != null) {
                torpedo.setDamage(ExperienceShellItem.applyDamageBonus(player, loadedTorpedo.getDamage()));
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
                    cooldowns.withSlotCooldown(player.getUUID(), weaponSlot, boostedCooldown, level.getGameTime()));
            launcherStack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                    WeaponCooldown.of(player.getUUID(), level.getGameTime(), boostedCooldown));
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4f, 0.4f);
    }
}
