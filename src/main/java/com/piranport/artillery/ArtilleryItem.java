package com.piranport.artillery;

import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.component.LoadedAmmo;
import com.piranport.debug.PiranPortDebug;
import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModDataComponents;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.entity.SlotAccess;
import java.util.List;

/**
 * 火炮物品基类。
 * Phase 1: 基础射击逻辑（右键发射直线炮弹）
 * Phase 2: 抛物线弹道 + 散布系统 + 调试模式
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

    /** 根据口径计算散布角（度）。口径越大散布越小。 */
    public float getDispersionAngle() {
        int cal = getCaliber();
        if (cal <= 4) return 1.5f;
        if (cal <= 8) return 1.0f;
        return 0.5f;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && !com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) {
            if (ShipCoreItem.tryFireFromInventory(level, player, hand)) {
                return InteractionResultHolder.consume(stack);
            }
        }

        if (!level.isClientSide && player.getAbilities().instabuild) {
            fireShell(level, player, stack);
            return InteractionResultHolder.consume(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    /** Phase 2: 发射物理炮弹（抛物线 + 散布） */
    private void fireShell(Level level, Player player, ItemStack stack) {
        ItemStack shellItem = new ItemStack(net.minecraft.world.item.Items.SNOWBALL);
        float speed = getInitialSpeed();
        float dispersion = getDispersionAngle();

        // 计算初速度方向（带散布）
        Vec3 direction = player.getLookAngle();
        if (dispersion > 0) {
            direction = applyDispersion(direction, level.random, dispersion);
        }

        // 创建物理炮弹
        ShellEntity shell = new ShellEntity(level, player, shellItem, getDamage(),
                speed, getDragCoeff(), getCustomGravity());
        shell.shoot(direction.x, direction.y, direction.z, speed, 0f);
        level.addFreshEntity(shell);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 1.5f);

        // Phase 2: 调试模式
        if (PiranPortDebug.isServerEnabled()) {
            showDebugInfo(player, direction, speed, getDragCoeff(), getCustomGravity());
            spawnPredictionTrail(level, player, direction, speed);
        }
    }

    /** 在初速度垂面内进行高斯散布偏移。 */
    private static Vec3 applyDispersion(Vec3 direction, RandomSource random, float dispersionDeg) {
        // 构建右手系
        Vec3 right = new Vec3(0, 1, 0).cross(direction).normalize();
        if (right.lengthSqr() < 0.001) {
            // 垂直看天/地时回退
            right = new Vec3(1, 0, 0);
        }
        Vec3 up = direction.cross(right).normalize();

        // 高斯分布在垂面内随机偏移
        double theta = random.nextDouble() * 2 * Math.PI;
        double r = random.nextGaussian() * dispersionDeg * 0.02; // 度→弧度系数
        return direction
                .add(right.scale(r * Math.cos(theta)))
                .add(up.scale(r * Math.sin(theta)))
                .normalize();
    }

    /** 调试信息：在 actionbar 显示弹道参数。 */
    private static void showDebugInfo(Player player, Vec3 direction,
                                       float speed, float drag, float gravity) {
        player.displayClientMessage(
                Component.literal(String.format(
                        "§7[弹道] v₀=%.1f  drag=%.3f  g=%.1f  θ=%.1f°",
                        speed, drag, gravity,
                        Math.toDegrees(Math.asin(direction.y))))
                        .withStyle(net.minecraft.ChatFormatting.GRAY),
                true);
    }

    /** 调试模式：用粒子绘制预测弹道。通过 ServerLevel 广播粒子给所有玩家。 */
    private static void spawnPredictionTrail(Level level, Player player, Vec3 direction, float speed) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        double px = player.getX(), py = player.getY() + player.getEyeHeight(), pz = player.getZ();
        double vx = direction.x * speed, vy = direction.y * speed, vz = direction.z * speed;
        double gravity = 0.05; // MC 单位

        for (int step = 0; step < 200; step++) {
            vx *= 0.99; // 简化阻力（与弹道匹配的简化预测）
            vy = vy * 0.99 - gravity;
            vz *= 0.99;
            px += vx; py += vy; pz += vz;

            if (step % 5 == 0) {
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.END_ROD,
                        px, py, pz, 1, 0, 0, 0, 0);
            }

            // 落地或超出范围停止
            if (py < player.getY() - 1 || step > 150) break;
        }
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
                // Phase 2: 调试模式下显示物理参数
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
