package com.piranport.artillery;

import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.artillery.config.ArtilleryConfig;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.WeaponCooldown;
import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

import net.minecraft.world.entity.SlotAccess;
import java.util.List;

/**
 * 火炮物品基类。
 * 继承 CannonItem 的 ShipCoreItem 兼容接口，同时支持独立右键发射 ShellEntity。
 */
public class ArtilleryItem extends Item {
    private final ArtilleryCannonData data;

    public ArtilleryItem(Properties properties, ArtilleryCannonData data) {
        super(properties);
        this.data = data;
    }

    public ArtilleryItem(Properties properties) {
        this(properties, ArtilleryCannonData.DEFAULT);
    }

    // ===== ShipCoreItem 兼容接口 =====
    public float getDamage() { return data.damage(); }
    public int getCooldownTicks() { return data.reloadTime(); }
    public int getBarrelCount() { return data.barrels(); }
    public int getCaliber() { return data.caliber(); }
    public ArtilleryCannonData getData() { return data; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Phase 1: 有 ShipCore GUI 时走背包模式，无 GUI 时独立发射
        if (!level.isClientSide && !com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) {
            if (ShipCoreItem.tryFireFromInventory(level, player, hand)) {
                return InteractionResultHolder.consume(stack);
            }
        }

        // 独立发射模式：创造模式直接发射炮弹
        if (!level.isClientSide && player.getAbilities().instabuild) {
            fireShell(level, player, stack);
            return InteractionResultHolder.consume(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    /** Phase 1: 发射直线炮弹 */
    private void fireShell(Level level, Player player, ItemStack stack) {
        ItemStack shellItem = new ItemStack(net.minecraft.world.item.Items.SNOWBALL);
        ShellEntity shell = new ShellEntity(level, player, shellItem, getDamage());
        shell.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 3.0f, 1.0f);
        level.addFreshEntity(shell);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 1.5f);
    }

    /** 手动装填（仅手动模式） */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot,
            ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY) return false;
        if (com.piranport.config.ModCommonConfig.AUTO_RESUPPLY_ENABLED.get()) return false;
        if (other.isEmpty() || !ShipCoreItem.matchesCaliber(other, stack)) return false;

        LoadedAmmo current = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (current.hasAmmo()) return false;
        if (other.getCount() < getBarrelCount()) return false;

        String ammoId = BuiltInRegistries.ITEM.getKey(other.getItem()).toString();
        stack.set(ModDataComponents.LOADED_AMMO.get(), new LoadedAmmo(getBarrelCount(), ammoId));
        com.piranport.debug.PiranPortDebug.consumeAmmo(other, getBarrelCount());

        if (!player.level().isClientSide()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 1.4f);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        var cat = stack.get(ModDataComponents.WEAPON_CATEGORY.get());
        if (cat != null) {
            tooltipComponents.add(Component.translatable("tooltip.piranport.weapon_category." + cat.getSerializedName())
                    .withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
        }

        LoadedAmmo loadedAmmo = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (loadedAmmo.hasAmmo()) {
            String ammoName = BuiltInRegistries.ITEM.get(ResourceLocation.parse(loadedAmmo.ammoItemId()))
                    .getDescription().getString();
            tooltipComponents.add(Component.translatable("tooltip.piranport.cannon.loaded_ammo",
                    loadedAmmo.count(), ammoName)
                    .withStyle(net.minecraft.ChatFormatting.GREEN));
        } else if (!com.piranport.config.ModCommonConfig.AUTO_RESUPPLY_ENABLED.get()) {
            tooltipComponents.add(Component.translatable("tooltip.piranport.cannon.no_ammo_loaded")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }

        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                tooltipComponents.add(Component.translatable("tooltip.piranport.cannon.barrel_count", getBarrelCount())
                        .withStyle(net.minecraft.ChatFormatting.AQUA));
                tooltipComponents.add(Component.translatable("tooltip.piranport.cannon.damage",
                        String.format("%.1f", getDamage())).withStyle(net.minecraft.ChatFormatting.RED));
                tooltipComponents.add(Component.translatable("tooltip.piranport.cooldown",
                        String.format("%.1f", getCooldownTicks() / 20.0)).withStyle(net.minecraft.ChatFormatting.YELLOW));
            } else {
                tooltipComponents.add(Component.translatable("tooltip.piranport.shift_for_details")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            }
        }
        ShipCoreItem.appendWeaponCooldownTooltip(stack, tooltipComponents);
    }
}
