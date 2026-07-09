package com.piranport.item;

import com.piranport.advancement.ModAdvancements;
import com.piranport.artillery.ArtilleryItem;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Consumable ruin reward that converts into player experience for equipment work. */
public class ExperienceShellItem extends TooltipItem {
    private static final int EXPERIENCE_POINTS = 240;
    public static final int MAX_ENHANCEMENT_LEVEL = 5;
    private static final int BASE_REPAIR_POINTS = 120;

    public ExperienceShellItem(Properties properties, String tooltipKey) {
        super(properties, tooltipKey);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack target = slot.getItem();
        if (stack.isEmpty() || target.isEmpty() || target.is(ModItems.EXP_SHELL.get())) return false;
        if (!isEnhanceableEquipment(target) && !target.isDamageableItem()) return false;

        if (player.level().isClientSide()) {
            return true;
        }

        if (target.isDamageableItem() && target.isDamaged()) {
            int repair = Math.min(target.getDamageValue(), getRepairAmount(target));
            target.setDamageValue(Math.max(0, target.getDamageValue() - repair));
            consumeOne(stack, player);
            awardUseAdvancement(player);
            player.displayClientMessage(Component.translatable(
                    "message.piranport.exp_shell_repaired", target.getHoverName(), repair), true);
            playEnhanceFeedback(player, ParticleTypes.WAX_ON, 14, 1.2f);
            return true;
        }

        if (!isEnhanceableEquipment(target)) {
            player.displayClientMessage(Component.translatable("message.piranport.exp_shell_not_equipment"), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.45f, 1.0f);
            return true;
        }

        int currentLevel = getEnhancementLevel(target);
        if (currentLevel >= MAX_ENHANCEMENT_LEVEL) {
            player.displayClientMessage(Component.translatable(
                    "message.piranport.exp_shell_max_level", target.getHoverName()), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.45f, 1.0f);
            return true;
        }

        int nextLevel = currentLevel + 1;
        target.set(ModDataComponents.EQUIPMENT_ENHANCEMENT_LEVEL.get(), nextLevel);
        consumeOne(stack, player);
        awardUseAdvancement(player);
        player.displayClientMessage(Component.translatable(
                "message.piranport.exp_shell_enhanced", target.getHoverName(), nextLevel), true);
        playEnhanceFeedback(player, ParticleTypes.ENCHANT, 22, 1.5f);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        player.giveExperiencePoints(EXPERIENCE_POINTS);
        consumeOne(stack, player);
        awardUseAdvancement(player);
        player.displayClientMessage(Component.translatable("message.piranport.exp_shell_used"), true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.4f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    player.getX(), player.getY() + 0.9, player.getZ(),
                    18, 0.35, 0.35, 0.35, 0.02);
        }

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.piranport.exp_shell.use")
                .withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.translatable("tooltip.piranport.exp_shell.equipment")
                .withStyle(ChatFormatting.AQUA));
    }

    public static int getEnhancementLevel(ItemStack stack) {
        return Math.max(0, Math.min(MAX_ENHANCEMENT_LEVEL,
                stack.getOrDefault(ModDataComponents.EQUIPMENT_ENHANCEMENT_LEVEL.get(), 0)));
    }

    public static float applyDamageBonus(ItemStack stack, float baseDamage) {
        return baseDamage * (1.0f + getEnhancementLevel(stack) * 0.06f);
    }

    public static float applyExplosionBonus(ItemStack stack, float baseExplosion) {
        return baseExplosion * (1.0f + getEnhancementLevel(stack) * 0.04f);
    }

    public static int applyCooldownReduction(ItemStack stack, int baseTicks) {
        int level = getEnhancementLevel(stack);
        if (level <= 0 || baseTicks <= 1) return baseTicks;
        float multiplier = Math.max(0.75f, 1.0f - level * 0.04f);
        return Math.max(1, Math.round(baseTicks * multiplier));
    }

    public static float applyAircraftPanelDamageBonus(ItemStack stack, float baseDamage) {
        return baseDamage * (1.0f + getEnhancementLevel(stack) * 0.05f);
    }

    public static float applyAircraftPanelSpeedBonus(ItemStack stack, float baseSpeed) {
        return baseSpeed * (1.0f + getEnhancementLevel(stack) * 0.03f);
    }

    public static void appendEnhancementTooltip(ItemStack stack, List<Component> tooltipComponents) {
        int level = getEnhancementLevel(stack);
        if (level > 0) {
            tooltipComponents.add(Component.translatable("tooltip.piranport.equipment_enhancement", level)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private static boolean isEnhanceableEquipment(ItemStack stack) {
        return stack.getItem() instanceof ArtilleryItem
                || stack.getItem() instanceof TorpedoLauncherItem
                || stack.getItem() instanceof MissileLauncherItem
                || stack.getItem() instanceof DepthChargeLauncherItem
                || stack.getItem() instanceof AircraftItem;
    }

    private static int getRepairAmount(ItemStack stack) {
        return Math.max(BASE_REPAIR_POINTS, Math.max(1, stack.getMaxDamage() / 5));
    }

    private static void consumeOne(ItemStack stack, Player player) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    private static void awardUseAdvancement(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.award(serverPlayer, "story/use_exp_shell");
        }
    }

    private static void playEnhanceFeedback(Player player, net.minecraft.core.particles.ParticleOptions particle,
                                            int count, float pitch) {
        Level level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.55f, pitch);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle,
                    player.getX(), player.getY() + 0.9, player.getZ(),
                    count, 0.35, 0.35, 0.35, 0.03);
        }
    }
}
