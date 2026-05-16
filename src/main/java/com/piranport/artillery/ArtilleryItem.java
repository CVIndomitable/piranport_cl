package com.piranport.artillery;

import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.SelectedAmmoType;
import com.piranport.component.WeaponCooldown;
import com.piranport.debug.PiranPortDebug;
import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 火炮物品基类。
 * Phase 1: 基础射击逻辑（右键发射直线炮弹）
 * Phase 2: 抛物线弹道 + 散布系统 + 调试模式
 * Phase 4: 统一自动装填，删除手动装填路径
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
    public float getInitialSpeed() { return data.initialSpeed(); }
    public float getDragCoeff() { return data.dragCoeff(); }
    public float getCustomGravity() { return data.gravity(); }
    public float getExplosionPower() { return data.explosionPower(); }

    /** 根据口径计算散布角（度）。口径越大散布越小。 */
    public float getDispersionAngle() {
        int cal = getCaliber();
        if (cal <= 4) return 1.5f;
        if (cal <= 8) return 1.0f;
        return 0.5f;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.IRON_INGOT);
    }

    /** Phase 12: 附魔能力（支持耐久/经验修补等原版附魔）。 */
    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isSpectator()) {
            return InteractionResultHolder.fail(stack);
        }

        if (level.isClientSide) {
            if (com.piranport.combat.TransformationManager.isPlayerTransformed(player)) {
                if (com.piranport.client.ClientScopeHandler.isScoping()) {
                    com.piranport.client.ClientScopeHandler.exitScope();
                } else {
                    com.piranport.client.ClientScopeHandler.enterScope(player, stack);
                }
                return InteractionResultHolder.fail(stack);
            }
            return InteractionResultHolder.pass(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    /** 右键手动装弹（手动补给模式下，右键弹药到火炮上装填一轮）。 */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot,
            ClickAction action, Player player, net.minecraft.world.entity.SlotAccess access) {
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

    /** 在初速度垂面内进行高斯散布偏移。散布角单位为度。 */
    public static Vec3 applyDispersion(Vec3 direction, RandomSource random, float dispersionDeg) {
        Vec3 right = new Vec3(0, 1, 0).cross(direction).normalize();
        if (right.lengthSqr() < 0.001) {
            right = new Vec3(1, 0, 0);
        }
        Vec3 up = direction.cross(right).normalize();

        double theta = random.nextDouble() * 2 * Math.PI;
        double r = random.nextGaussian() * dispersionDeg * 0.02;
        return direction
                .add(right.scale(r * Math.cos(theta)))
                .add(up.scale(r * Math.sin(theta)))
                .normalize();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        var cat = stack.get(ModDataComponents.WEAPON_CATEGORY.get());
        if (cat != null) {
            tooltipComponents.add(Component.translatable("tooltip.piranport.weapon_category." + cat.getSerializedName())
                    .withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
        }

        // 耐久状态
        if (stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            tooltipComponents.add(Component.translatable("tooltip.piranport.cannon.damaged")
                    .withStyle(net.minecraft.ChatFormatting.RED));
        } else if (stack.isDamageableItem()) {
            int cur = stack.getMaxDamage() - stack.getDamageValue();
            int max = stack.getMaxDamage();
            tooltipComponents.add(Component.translatable("tooltip.piranport.durability",
                    String.valueOf(cur), String.valueOf(max))
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }

        // 当前选中弹种
        SelectedAmmoType selected = stack.getOrDefault(ModDataComponents.SELECTED_AMMO_TYPE.get(), SelectedAmmoType.EMPTY);
        if (selected.hasSelection()) {
            Item ammoItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(selected.ammoItemId()));
            tooltipComponents.add(Component.translatable("tooltip.piranport.cannon.selected_ammo",
                    ammoItem.getDescription().getString())
                    .withStyle(net.minecraft.ChatFormatting.YELLOW));
        }

        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                tooltipComponents.add(Component.translatable("tooltip.piranport.cannon.barrel_count", getBarrelCount())
                        .withStyle(net.minecraft.ChatFormatting.AQUA));
                tooltipComponents.add(Component.translatable("tooltip.piranport.cannon.damage",
                        String.format("%.1f", getDamage())).withStyle(net.minecraft.ChatFormatting.RED));
                tooltipComponents.add(Component.translatable("tooltip.piranport.cooldown",
                        String.format("%.1f", getCooldownTicks() / 20.0)).withStyle(net.minecraft.ChatFormatting.YELLOW));

                // 装填进度
                WeaponCooldown cd = stack.get(ModDataComponents.WEAPON_COOLDOWN.get());
                if (cd != null) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level != null) {
                        float fraction = cd.getFraction(mc.level.getGameTime());
                        if (fraction > 0f) {
                            int pct = Math.round((1f - fraction) * 100);
                            tooltipComponents.add(Component.translatable("tooltip.piranport.cannon.reload_progress",
                                    pct).withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
                        }
                    }
                }

                if (PiranPortDebug.isClientEnabled()) {
                    tooltipComponents.add(Component.literal(String.format("§8v₀=%.1f drag=%.3f g=%.1f",
                            getInitialSpeed(), getDragCoeff(), getCustomGravity()))
                            .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
                    tooltipComponents.add(Component.literal(String.format("§8散布=%.1f° 口径=%d",
                            getDispersionAngle(), getCaliber()))
                            .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
                }
            } else {
                tooltipComponents.add(Component.translatable("tooltip.piranport.shift_for_details")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            }
        }
        ShipCoreItem.appendWeaponCooldownTooltip(stack, tooltipComponents);
    }
}
