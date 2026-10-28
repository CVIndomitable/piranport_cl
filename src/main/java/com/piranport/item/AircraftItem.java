package com.piranport.item;

import com.piranport.combat.TransformationManager;
import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.AircraftDefinitionService;
import com.piranport.aviation.AircraftSortieReadiness;
import com.piranport.aviation.AircraftStatsService;
import com.piranport.aviation.ResolvedAircraftStats;
import com.piranport.component.AircraftState;
import com.piranport.component.WeaponCategory;
import com.piranport.component.WeaponCooldown;
import com.piranport.platform.ClientHooks;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class AircraftItem extends Item {

    public AircraftItem(Properties properties) {
        super(properties);
    }

    /** 平时隐藏原版耐久条，避免盖住图标底部的两态标识；只在损坏时显示空条。 */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return WeaponDurabilityBar.showWhenBroken(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity,
                              int slot, boolean selected) {
        // 装填读条计时存在物品自己身上（与火炮同组件）。物品被移出快捷栏时
        // WeaponReloadLifecycle 负责清掉未完成的读条，避免计时跟着物品跑到别处。
        com.piranport.combat.data.WeaponReloadLifecycle.tick(stack, level, entity, slot);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ShipCoreCombat.tryFireFromInventory(level, player, hand)) {
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }


    /**
     * Right-click fuel onto aircraft in inventory to manually load fuel (manual reload mode only).
     * One fuel item fills the aircraft to full capacity.
     */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot,
            ClickAction action, Player player, net.minecraft.world.entity.SlotAccess access) {
        if (action != ClickAction.SECONDARY) return false;
        if (!other.is(ModItems.FUEL.get())) return false;

        AircraftDefinition definition = AircraftDefinitionService.resolve(stack);
        if (definition == null) return false;
        AircraftState state = AircraftDefinitionService.stateOf(stack);
        int fuelCapacity = AircraftStatsService.resolve(definition).fuelCapacity();
        if (state.currentFuel() >= fuelCapacity) return false;

        stack.set(ModDataComponents.AIRCRAFT_STATE.get(), state.withCurrentFuel(fuelCapacity));
        other.shrink(1);

        if (!player.level().isClientSide) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.5f, 1.2f);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        ExperienceShellItem.appendEnhancementTooltip(stack, tooltipComponents);

        WeaponCategory cat = stack.get(ModDataComponents.WEAPON_CATEGORY.get());
        if (cat != null) {
            tooltipComponents.add(Component.translatable("tooltip.piranport.weapon_category." + cat.getSerializedName())
                    .withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
        }
        AircraftDefinition definition = AircraftDefinitionService.resolve(stack);
        if (definition != null) {
            ResolvedAircraftStats stats = AircraftStatsService.resolve(definition);
            AircraftState state = AircraftDefinitionService.stateOf(stack);
            tooltipComponents.add(Component.translatable(
                            "tooltip.piranport.aircraft_type."
                                    + definition.aircraftClass().getSerializedName())
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
            if (ClientHooks.isClient() && ClientHooks.hasShiftDown()) {
                if (stats.damage() > 0) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.aircraft_damage",
                            String.format("%.1f", stats.damage()))
                            .withStyle(net.minecraft.ChatFormatting.RED));
                }
                tooltipComponents.add(Component.translatable("tooltip.piranport.aircraft_speed",
                        String.format("%.1f", stats.speed()))
                        .withStyle(net.minecraft.ChatFormatting.GREEN));
            } else if (ClientHooks.isClient()) {
                tooltipComponents.add(Component.translatable("tooltip.piranport.shift_for_details")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            }
            // 一架飞机只输出一条状态：装填中 > 已装填 > 未装填。
            // WHY 由这里统一负责：图标谓词（ClientModEvents 的 fueled）也用
            // AircraftSortieReadiness 判定，两处必须同源；装填读条期间若再让
            // appendWeaponCooldownTooltip 追加一行，就会出现「未装填 + 装填中」两条。
            boolean ready = AircraftSortieReadiness.isReady(state, definition, stats);
            WeaponCooldown cooldown = stack.get(ModDataComponents.WEAPON_COOLDOWN.get());
            Level level = context.level();
            boolean reloading = level != null && cooldown != null
                    && cooldown.isOnCooldown(level.getGameTime());
            if (reloading) {
                tooltipComponents.add(Component.translatable("tooltip.piranport.weapon_reloading")
                        .withStyle(net.minecraft.ChatFormatting.YELLOW));
            } else {
                tooltipComponents.add(Component.translatable(ready
                                ? "tooltip.piranport.weapon_ready"
                                : "tooltip.piranport.weapon_not_loaded")
                        .withStyle(ready ? net.minecraft.ChatFormatting.GREEN
                                : net.minecraft.ChatFormatting.RED));
            }
        }
        // 状态行已由本方法输出；不要再调用 appendWeaponCooldownTooltip 追加第二行。
    }
}
