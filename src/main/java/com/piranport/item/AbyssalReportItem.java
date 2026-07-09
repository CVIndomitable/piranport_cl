package com.piranport.item;

import com.piranport.advancement.ModAdvancements;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Loot report from abyssal ruins. Reading it grants a short exploration buff
 * and prints one piece of abyssal intel.
 */
public class AbyssalReportItem extends TooltipItem {
    private static final int COOLDOWN_TICKS = 20 * 180;

    public AbyssalReportItem(Properties properties, String tooltipKey) {
        super(properties, tooltipKey);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 240, 0));
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 20 * 240, 0));
        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 20 * 40, 0));
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        int line = level.random.nextInt(4) + 1;
        player.displayClientMessage(Component.translatable("message.piranport.abyssal_report_" + line), true);
        if (player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.award(serverPlayer, "story/read_abyssal_report");
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 0.85f + level.random.nextFloat() * 0.2f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    20, 0.45, 0.35, 0.45, 0.02);
        }

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.piranport.abyssal_report.use")
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
