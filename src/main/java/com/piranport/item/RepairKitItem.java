package com.piranport.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class RepairKitItem extends Item {

    private static final double RANGE = 5.0;
    private static final int REGEN_DURATION = 60; // 3 seconds, refreshed each tick while held
    private static final int REGEN_AMPLIFIER = 1;  // Level II

    public RepairKitItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000; // essentially infinite, like a bow
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Start continuous use if looking at a living entity within range
        LivingEntity target = getTargetEntity(player);
        if (target != null) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(user instanceof Player player)) return;

        LivingEntity target = getTargetEntity(player);
        if (target == null) {
            player.stopUsingItem();
            return;
        }

        target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_DURATION, REGEN_AMPLIFIER, false, true));

        // Consume durability every 20 ticks (1 second)
        int usedTicks = getUseDuration(stack, user) - remainingUseDuration;
        if (usedTicks % 20 == 0) {
            stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack));
            level.playSound(null, target.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR,
                    SoundSource.PLAYERS, 0.6F, 1.0F);
        }
    }

    @Nullable
    private LivingEntity getTargetEntity(Player player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookVec.scale(RANGE));
        AABB searchArea = player.getBoundingBox().expandTowards(lookVec.scale(RANGE)).inflate(1.0);
        EntityHitResult result = ProjectileUtil.getEntityHitResult(
                player, eyePos, endPos, searchArea,
                e -> e instanceof LivingEntity && !e.isSpectator() && e.isAlive()
                        && !(e instanceof net.minecraft.world.entity.monster.Monster),
                RANGE * RANGE
        );
        if (result != null && result.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }
}
