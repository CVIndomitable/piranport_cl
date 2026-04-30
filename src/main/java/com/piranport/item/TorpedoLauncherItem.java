package com.piranport.item;

import com.piranport.component.LoadedAmmo;
import com.piranport.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.piranport.component.WeaponCategory;

import java.util.List;

public class TorpedoLauncherItem extends Item {
    private final int caliber;
    private final int tubeCount;
    private final int cooldownTicks;

    public TorpedoLauncherItem(Properties properties, int caliber, int tubeCount, int cooldownTicks) {
        super(properties);
        this.caliber = caliber;
        this.tubeCount = tubeCount;
        this.cooldownTicks = cooldownTicks;
    }

    public int getCaliber() {
        return caliber;
    }

    public int getTubeCount() {
        return tubeCount;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ShipCoreItem.tryFireFromInventory(level, player, hand)) {
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    /**
     * Right-click manual loading: only allowed when 鱼雷再装填 enhancement is equipped.
     * Otherwise must use 装填设施 (Reload Facility) machine block.
     */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot,
            ClickAction action, Player player, net.minecraft.world.entity.SlotAccess access) {
        if (action != ClickAction.SECONDARY) return false;
        if (other.isEmpty() || !(other.getItem() instanceof TorpedoItem torpedo)) return false;
        if (torpedo.getCaliber() != caliber) return false;

        // Check if player has transformed core with 鱼雷再装填 enhancement
        ItemStack coreStack = ItemStack.EMPTY;
        for (ItemStack s : player.getInventory().items) {
            if (s.getItem() instanceof ShipCoreItem && com.piranport.combat.TransformationManager.isTransformed(s)) {
                coreStack = s;
                break;
            }
        }
        if (coreStack.isEmpty()) {
            ItemStack offhand = player.getInventory().offhand.get(0);
            if (offhand.getItem() instanceof ShipCoreItem && com.piranport.combat.TransformationManager.isTransformed(offhand)) {
                coreStack = offhand;
            }
        }
        if (coreStack.isEmpty()) return false;
        if (!com.piranport.combat.TransformationManager.hasTorpedoReloadEquipped(player, coreStack)) {
            return false;
        }

        // Check if already fully loaded
        LoadedAmmo loaded = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (loaded.hasAmmo() && loaded.count() >= tubeCount) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.0f);
            return false;
        }

        // Check ammo type consistency
        String newAmmoId = BuiltInRegistries.ITEM.getKey(other.getItem()).toString();
        if (loaded.hasAmmo()) {
            String existingAmmoId = loaded.ammoItemId();
            if (!existingAmmoId.equals(newAmmoId)) {
                // Eject existing torpedoes and load new type
                Item existingItem = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(existingAmmoId));
                ItemStack ejectedStack = new ItemStack(existingItem, loaded.count());
                if (!player.getInventory().add(ejectedStack)) {
                    player.drop(ejectedStack, false);
                }
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 0.8f);

                // Load new torpedoes
                int toLoad = Math.min(tubeCount, other.getCount());
                stack.set(ModDataComponents.LOADED_AMMO.get(), new LoadedAmmo(toLoad, newAmmoId));
                other.shrink(toLoad);

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 1.4f);
                return true;
            }
        }

        // Load torpedoes (same type or empty launcher)
        int currentCount = loaded.hasAmmo() ? loaded.count() : 0;
        int space = tubeCount - currentCount;
        int toLoad = Math.min(space, other.getCount());
        stack.set(ModDataComponents.LOADED_AMMO.get(), new LoadedAmmo(currentCount + toLoad, newAmmoId));
        other.shrink(toLoad);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 1.4f);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        WeaponCategory cat = stack.get(ModDataComponents.WEAPON_CATEGORY.get());
        if (cat != null) {
            tooltipComponents.add(Component.translatable("tooltip.piranport.weapon_category." + cat.getSerializedName())
                    .withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
        }

        LoadedAmmo loadedAmmo = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (loadedAmmo.hasAmmo()) {
            String ammoName = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(loadedAmmo.ammoItemId()))
                    .getDescription().getString();
            tooltipComponents.add(Component.translatable("tooltip.piranport.launcher.loaded_ammo",
                    loadedAmmo.count(), ammoName)
                    .withStyle(net.minecraft.ChatFormatting.GREEN));
        } else if (!com.piranport.config.ModCommonConfig.AUTO_RESUPPLY_ENABLED.get()) {
            tooltipComponents.add(Component.translatable("tooltip.piranport.launcher.no_ammo_loaded")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }

        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                tooltipComponents.add(Component.translatable("tooltip.piranport.launcher.caliber", caliber)
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
                tooltipComponents.add(Component.translatable("tooltip.piranport.launcher.tubes", tubeCount)
                        .withStyle(net.minecraft.ChatFormatting.AQUA));
                tooltipComponents.add(Component.translatable("tooltip.piranport.cooldown",
                        String.format("%.1f", cooldownTicks / 20.0)).withStyle(net.minecraft.ChatFormatting.YELLOW));
                if (stack.isDamageableItem()) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.durability",
                            stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage())
                            .withStyle(net.minecraft.ChatFormatting.GREEN));
                }
            } else {
                tooltipComponents.add(Component.translatable("tooltip.piranport.shift_for_details")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            }
        }
        ShipCoreItem.appendWeaponCooldownTooltip(stack, tooltipComponents);
    }
}
