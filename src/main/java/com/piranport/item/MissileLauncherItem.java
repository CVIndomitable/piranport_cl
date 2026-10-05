package com.piranport.item;

import com.piranport.entity.MissileEntity;
import com.piranport.platform.ClientHooks;
import com.piranport.terminal.TerminalParameters;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/**
 * 导弹/火箭弹发射器。
 * <p>
 * 反舰导弹 / 火箭弹：可连续发射 burstCount 次（无冷却），仅在装填设施装填。
 * 防空导弹：每次发射后 cooldownTicks 冷却，自动从弹药库消耗弹药。
 */
public class MissileLauncherItem extends Item {

    private final MissileEntity.MissileType missileType;
    private final float damage;
    private final float armorPen;
    private final float explosionPower;
    private final int burstCount;
    private final int cooldownTicks;
    private final Supplier<Item> ammoItem;

    public MissileLauncherItem(Properties properties, MissileEntity.MissileType missileType,
                                float damage, float armorPen, float explosionPower,
                                int burstCount, int cooldownTicks, Supplier<Item> ammoItem) {
        super(properties);
        this.missileType = missileType;
        this.damage = damage;
        this.armorPen = armorPen;
        this.explosionPower = explosionPower;
        this.burstCount = burstCount;
        this.cooldownTicks = cooldownTicks;
        this.ammoItem = ammoItem;
    }

    public MissileEntity.MissileType getMissileType() { return missileType; }

    // 注册数值（伤害/穿甲/爆炸/连装/冷却）可在调试终端按 item 覆盖，键：
    // equipment.<注册名>.<属性>（与雷达/声纳同一套约定）。目录侧用 getBase* 读基准值，
    // 玩法侧与 tooltip 用下面的 get* 读覆盖值，避免「能改但不生效」。
    public float getDamage() { return (float) TerminalParameters.getDouble(parameterKey("damage"), damage); }
    public float getBaseDamage() { return damage; }

    public float getArmorPen() { return (float) TerminalParameters.getDouble(parameterKey("armor_pen"), armorPen); }
    public float getBaseArmorPen() { return armorPen; }

    public float getExplosionPower() {
        return (float) TerminalParameters.getDouble(parameterKey("explosion_power"), explosionPower);
    }
    public float getBaseExplosionPower() { return explosionPower; }

    public int getBurstCount() { return TerminalParameters.getInt(parameterKey("burst_count"), burstCount); }
    public int getBaseBurstCount() { return burstCount; }

    public int getCooldownTicks() { return TerminalParameters.getInt(parameterKey("fire_cooldown"), cooldownTicks); }
    public int getBaseCooldownTicks() { return cooldownTicks; }

    public Item getAmmoItem() { return ammoItem.get(); }

    private String parameterKey(String property) {
        var id = BuiltInRegistries.ITEM.getKey(this);
        return "equipment." + (id == null ? "missile_launcher" : id) + "." + property;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity,
                              int slot, boolean selected) {
        com.piranport.combat.data.WeaponReloadLifecycle.tick(stack, level, entity, slot);
    }

    /** 是否为手动装填模式（反舰/火箭：需要在装填设施装弹）。 */
    public boolean isManualReload() {
        return missileType == MissileEntity.MissileType.ANTI_SHIP
                || missileType == MissileEntity.MissileType.ROCKET;
    }

    /**
     * 装填设施是否可装填此发射器。
     * 防空导弹发射器自动装填，明确禁止放入装填设施以防玩家误操作。
     */
    public boolean canReloadInFacility() {
        return missileType != MissileEntity.MissileType.ANTI_AIR
                && isManualReload();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ShipCoreCombat.tryFireFromInventory(level, player, hand)) {
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        ExperienceShellItem.appendEnhancementTooltip(stack, tooltipComponents);

        // 导弹类型（always visible）
        tooltipComponents.add(Component.translatable(missileType.translationKey)
                .withStyle(ChatFormatting.GRAY));

        if (ClientHooks.isClient()) {
            if (ClientHooks.hasShiftDown()) {
                // 读覆盖后的生效值（终端可改），而非字段基准值
                float effDamage = getDamage();
                float effArmorPen = getArmorPen();
                int effBurst = getBurstCount();
                int effCooldown = getCooldownTicks();
                // 伤害
                if (effArmorPen > 0) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.missile.damage_ap",
                            String.format("%.0f", effDamage), String.format("%.0f", effArmorPen))
                            .withStyle(ChatFormatting.RED));
                } else {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.missile.damage",
                            String.format("%.0f", effDamage)).withStyle(ChatFormatting.RED));
                }
                // 连装数（反舰/火箭）
                if (isManualReload() && effBurst > 1) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.missile.burst", effBurst)
                            .withStyle(ChatFormatting.AQUA));
                }
                // 爆炸（防空/火箭）
                if (missileType != MissileEntity.MissileType.ANTI_SHIP) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.missile.explosion",
                            String.format("%.1f", getExplosionPower())).withStyle(ChatFormatting.GOLD));
                }
                // 装填时间（防空导弹有冷却）
                if (effCooldown > 0) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.cooldown",
                            String.format("%.1f", effCooldown / 20.0)).withStyle(ChatFormatting.YELLOW));
                }
            } else {
                tooltipComponents.add(Component.translatable("tooltip.piranport.shift_for_details")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        ShipCoreCombat.appendWeaponCooldownTooltip(stack, tooltipComponents);
    }
}
