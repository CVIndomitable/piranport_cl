package com.piranport.item;

import com.piranport.config.ModEquipmentConfig;
import com.piranport.config.TerminalConfigValue;
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

    // 注册数值（伤害/穿甲/爆炸/连装/冷却）可在调试终端覆盖。每个型号独立键：
    // missile_launcher.<注册路径>.<属性>（独立分类「导弹」，不再与雷达/声纳挤在 equipment）。
    // 三层优先级：单型号键（被策划设置过）> 共享默认键 global.missile_launcher.<属性> > 物品注册基准。
    // 判定「设置过」必须走 TerminalParameters.isOverridden（覆盖表成员），不能靠 getXxx 的返回值：
    // 客户端 clientValues 会把未设置的键填成 spec 基准值，用值判定会在客户端误命中共享层。
    // 目录侧用 getBase* 读基准值，玩法侧与 tooltip 用下面的 get* 读生效值，避免「能改但不生效」。
    public float getDamage() {
        return (float) effectiveDouble("damage", ModEquipmentConfig.MISSILE_LAUNCHER_DAMAGE, damage);
    }
    public float getBaseDamage() { return damage; }

    public float getArmorPen() {
        return (float) effectiveDouble("armor_pen", ModEquipmentConfig.MISSILE_LAUNCHER_ARMOR_PEN, armorPen);
    }
    public float getBaseArmorPen() { return armorPen; }

    public float getExplosionPower() {
        return (float) effectiveDouble("explosion_power",
                ModEquipmentConfig.MISSILE_LAUNCHER_EXPLOSION_POWER, explosionPower);
    }
    public float getBaseExplosionPower() { return explosionPower; }

    public int getBurstCount() {
        return effectiveInt("burst_count", ModEquipmentConfig.MISSILE_LAUNCHER_BURST_COUNT, burstCount);
    }
    public int getBaseBurstCount() { return burstCount; }

    public int getCooldownTicks() {
        return effectiveInt("fire_cooldown", ModEquipmentConfig.MISSILE_LAUNCHER_FIRE_COOLDOWN, cooldownTicks);
    }
    public int getBaseCooldownTicks() { return cooldownTicks; }

    public Item getAmmoItem() { return ammoItem.get(); }

    /** 单型号覆盖 &gt; 共享默认 &gt; 物品基准。 */
    private double effectiveDouble(String property, TerminalConfigValue<Double> shared, double base) {
        String key = parameterKey(property);
        if (TerminalParameters.isOverridden(key)) return TerminalParameters.getDouble(key, base);
        return shared.isSet() ? shared.get() : base;
    }

    /** 单型号覆盖 &gt; 共享默认 &gt; 物品基准。 */
    private int effectiveInt(String property, TerminalConfigValue<Integer> shared, int base) {
        String key = parameterKey(property);
        if (TerminalParameters.isOverridden(key)) return TerminalParameters.getInt(key, base);
        return shared.isSet() ? shared.get() : base;
    }

    private String parameterKey(String property) {
        var id = BuiltInRegistries.ITEM.getKey(this);
        // 去命名空间：target 用注册路径（sy1_launcher），与目录侧 TerminalParameterCatalog 一致。
        return "missile_launcher." + (id == null ? "missile_launcher" : id.getPath()) + "." + property;
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
