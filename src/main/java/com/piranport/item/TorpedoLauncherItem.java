package com.piranport.item;

import com.piranport.component.LoadedAmmo;
import com.piranport.platform.ClientHooks;
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
import java.util.function.IntSupplier;

public class TorpedoLauncherItem extends Item {
    private final int caliber;
    private final int tubeCount;
    private final IntSupplier cooldownSupplier;
    /** 稀有度档（策划决策/数值/08）；只做分类展示，不改负重。 */
    private final com.piranport.component.EquipmentTier tier;

    public TorpedoLauncherItem(Properties properties, int caliber, int tubeCount, IntSupplier cooldownSupplier) {
        this(properties, caliber, tubeCount, cooldownSupplier, com.piranport.component.EquipmentTier.INITIAL);
    }

    public TorpedoLauncherItem(Properties properties, int caliber, int tubeCount, IntSupplier cooldownSupplier,
                               com.piranport.component.EquipmentTier tier) {
        super(properties);
        this.caliber = caliber;
        this.tubeCount = tubeCount;
        this.cooldownSupplier = cooldownSupplier;
        this.tier = tier;
    }

    public com.piranport.component.EquipmentTier getTier() {
        return tier;
    }

    /** 发射器负重：ceil((1 + 联装) × 口径英寸² × 0.00275)。只算管子不算弹种（数值/08）。 */
    public int getWeight() {
        return computeWeight(caliber, tubeCount);
    }

    /** 毫米 → 公式英寸：533→21、610→24、720→28；其他口径按 /25.4 四舍五入到整英寸。 */
    public static int caliberInches(int caliberMm) {
        return switch (caliberMm) {
            case 533 -> 21;
            case 610 -> 24;
            case 720 -> 28;
            default -> (int) Math.round(caliberMm / 25.4);
        };
    }

    public static int computeWeight(int caliberMm, int tubes) {
        int inches = caliberInches(caliberMm);
        // 系数 0.00275 = 原 0.0055 的一半（2026-10-18 全局减半）。
        // 用整数运算避开浮点误差：ceil((1+n)·c²·55 / 20000)
        long numerator = (long) (1 + tubes) * inches * inches * 55L;
        return (int) ((numerator + 19999L) / 20000L);
    }

    public int getCaliber() {
        return caliber;
    }

    public int getTubeCount() {
        return tubeCount;
    }

    /** 供应商返回 0/负数/离谱值时使用的合法兜底（三联装默认冷却）。 */
    public static final int FALLBACK_COOLDOWN_TICKS = 100;
    /** 超过这个值的冷却视为坏数据：不能让它变成「永不到期」的读条。 */
    public static final int MAX_REASONABLE_COOLDOWN_TICKS = 12_000;

    public static int sanitizeCooldownTicks(int ticks) {
        return ticks > 0 && ticks <= MAX_REASONABLE_COOLDOWN_TICKS ? ticks : FALLBACK_COOLDOWN_TICKS;
    }

    public int getCooldownTicks() {
        return sanitizeCooldownTicks(cooldownSupplier.getAsInt());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity,
                              int slot, boolean selected) {
        com.piranport.combat.data.WeaponReloadLifecycle.tick(stack, level, entity, slot);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // 返回值只表示"船上有没有核心/武器"，发射策略全是 void，因此这里恒为 true，
        // consume 与否不改变实际开火结果——真正的拒绝信息由策略自己 actionbar 播报。
        if (ShipCoreCombat.tryFireFromInventory(level, player, hand)) {
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

        // R 读条期间不能再拖入装填：PENDING 到期会再扣一组弹药，与手动拖入叠加会重复扣弹。
        if (stack.get(ModDataComponents.PENDING_TORPEDO_RELOAD.get()) != null) {
            player.displayClientMessage(Component.translatable("message.piranport.already_reloading"), true);
            return false;
        }

        // Check if player has transformed core with 鱼雷再装填 enhancement
        ItemStack coreStack = com.piranport.combat.TransformationManager.findTransformedCore(player);
        if (coreStack.isEmpty()) return false;
        if (!com.piranport.combat.TransformationManager.hasTorpedoReloadEquipped(player, coreStack)) {
            return false;
        }

        // Check if already fully loaded
        LoadedAmmo loaded = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        int currentCount = loaded.hasAmmo() ? loaded.count() : 0;
        String newAmmoId = BuiltInRegistries.ITEM.getKey(other.getItem()).toString();

        // 全满换弹：已装满且弹药类型不同
        if (currentCount >= tubeCount) {
            if (loaded.hasAmmo() && !loaded.ammoItemId().equals(newAmmoId)) {
                // 要求新弹药数量足够完全装填
                if (other.getCount() < tubeCount) {
                    player.displayClientMessage(Component.translatable("message.piranport.insufficient_ammo_for_full_load"), true);
                    return false;
                }

                // 弹出现有弹药
                Item existingItem = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(loaded.ammoItemId()));
                ItemStack ejectedStack = new ItemStack(existingItem, loaded.count());
                if (!player.getInventory().add(ejectedStack)) {
                    player.drop(ejectedStack, false);
                }
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 0.8f);

                // 装填新弹药（满管）
                stack.set(ModDataComponents.LOADED_AMMO.get(), new LoadedAmmo(tubeCount, newAmmoId));
                other.shrink(tubeCount);

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 1.4f);
                return true;
            }
            // 已满且同类型，拒绝
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.0f);
            return false;
        }

        // 部分装填状态：拒绝装填
        if (currentCount > 0) {
            player.displayClientMessage(Component.translatable("message.piranport.must_be_empty_to_reload"), true);
            return false;
        }

        // 空载：要求完全装填
        if (other.getCount() < tubeCount) {
            player.displayClientMessage(Component.translatable("message.piranport.insufficient_ammo_for_full_load"), true);
            return false;
        }

        // 装填满管
        stack.set(ModDataComponents.LOADED_AMMO.get(), new LoadedAmmo(tubeCount, newAmmoId));
        other.shrink(tubeCount);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 1.4f);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        ExperienceShellItem.appendEnhancementTooltip(stack, tooltipComponents);

        // 空膛不再显示「未装填鱼雷」：空膛/装填由物品模型的 loaded/empty 图标切换表达，
        // 提示只在真正装了雷时给出弹种与数量。
        LoadedAmmo loadedAmmo = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (loadedAmmo.hasAmmo()) {
            String ammoName = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(loadedAmmo.ammoItemId()))
                    .getDescription().getString();
            tooltipComponents.add(Component.translatable("tooltip.piranport.launcher.loaded_ammo",
                    loadedAmmo.count(), ammoName)
                    .withStyle(net.minecraft.ChatFormatting.GREEN));
        }

        // 数值/08：说明必须写明口径和联装（不藏在 Shift 后）
        tooltipComponents.add(Component.translatable("tooltip.piranport.launcher.spec", caliber, tubeCount)
                .withStyle(net.minecraft.ChatFormatting.GRAY));

        if (ClientHooks.isClient()) {
            if (ClientHooks.hasShiftDown()) {
                // 类型/稀有度折叠进 Shift 详情；口径与联装仍由上方常驻的 launcher.spec 显示。
                WeaponCategory cat = stack.get(ModDataComponents.WEAPON_CATEGORY.get());
                if (cat != null) {
                    tooltipComponents.add(Component.translatable(
                                    "tooltip.piranport.weapon_category." + cat.getSerializedName())
                            .withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
                }
                tooltipComponents.add(tier.tooltip());
                tooltipComponents.add(Component.translatable("tooltip.piranport.cooldown",
                        String.format("%.1f", getCooldownTicks() / 20.0)).withStyle(net.minecraft.ChatFormatting.YELLOW));
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
        ShipCoreCombat.appendWeaponCooldownTooltip(stack, tooltipComponents);
    }
}
