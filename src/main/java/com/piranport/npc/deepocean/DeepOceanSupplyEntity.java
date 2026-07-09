package com.piranport.npc.deepocean;

import com.piranport.npc.ai.goal.FleetAlertGoal;
import com.piranport.npc.ai.goal.IdleWanderGoal;
import com.piranport.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 补给酱 — Non-combat supply entity. Does not attack. Drops resources on death.
 * HP:20 Armor:0 Speed:1.2 Detection:16
 */
public class DeepOceanSupplyEntity extends AbstractDeepOceanEntity {
    private static final int SUPPORT_INTERVAL_TICKS = 80;
    private static final double SUPPORT_RADIUS = 8.0;
    private static final float SUPPORT_HEAL_AMOUNT = 3.0f;
    private static final int SUPPORT_TARGET_LIMIT = 2;

    public DeepOceanSupplyEntity(EntityType<? extends DeepOceanSupplyEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)  // 1.2x relative speed
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
                .add(Attributes.ARMOR, 0.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new IdleWanderGoal(this, 0.8, 32));
        this.goalSelector.addGoal(5, new FleetAlertGoal(this));
        // No attack or target goals — supply ships don't fight
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && tickCount % SUPPORT_INTERVAL_TICKS == 0) {
            supportNearbyFleet();
        }
    }

    @Override
    public double getOrbitDistance() {
        return 0; // Does not orbit
    }

    private void supportNearbyFleet() {
        int healed = 0;
        for (AbstractDeepOceanEntity ally : level().getEntitiesOfClass(
                AbstractDeepOceanEntity.class,
                getBoundingBox().inflate(SUPPORT_RADIUS),
                ally -> ally != this && ally.isAlive() && ally.getHealth() < ally.getMaxHealth())) {
            ally.heal(SUPPORT_HEAL_AMOUNT);
            healed++;
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.BUBBLE_POP,
                        ally.getX(), ally.getY() + 0.7, ally.getZ(),
                        8, 0.25, 0.25, 0.25, 0.02);
            }
            if (healed >= SUPPORT_TARGET_LIMIT) {
                break;
            }
        }

        if (healed > 0) {
            level().playSound(null, blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT,
                    SoundSource.NEUTRAL, 0.35f, 1.35f);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.ABYSSAL_REPORT.get())) {
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            giveOrDrop(player, new ItemStack(ModItems.RAW_ALUMINUM.get(), 4));
            giveOrDrop(player, new ItemStack(ModItems.AVIATION_FUEL.get(), 2));
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 20 * 90, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_supply_intel_trade"));
            playTradeFeedback();
            return InteractionResult.SUCCESS;
        }

        if (held.is(ModItems.FUEL.get())) {
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            heal(8.0f);
            giveOrDrop(player, new ItemStack(ModItems.REPAIR_KIT.get(), 1));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_supply_fuel_trade"));
            playTradeFeedback();
            return InteractionResult.SUCCESS;
        }

        if (isChaosShard(held)) {
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            giveOrDrop(player, new ItemStack(ModItems.RAW_ALUMINUM.get(), 10));
            giveOrDrop(player, new ItemStack(ModItems.EXP_SHELL.get(), 1));
            player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 20 * 120, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_supply_shard_trade"));
            playTradeFeedback();
            return InteractionResult.SUCCESS;
        }

        if (isNationalFlag(held)) {
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            giveOrDrop(player, new ItemStack(ModItems.AVIATION_FUEL.get(), 4));
            giveOrDrop(player, new ItemStack(ModItems.REPAIR_KIT.get(), 1));
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 20 * 80, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_supply_flag_trade"));
            playTradeFeedback();
            return InteractionResult.SUCCESS;
        }

        if (held.is(ModItems.EXP_SHELL.get())) {
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            heal(12.0f);
            giveOrDrop(player, new ItemStack(ModItems.ABYSSAL_REPORT.get(), 1));
            giveOrDrop(player, new ItemStack(ModItems.AVIATION_FUEL.get(), 2));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 60, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_supply_drill_trade"));
            playTradeFeedback();
            return InteractionResult.SUCCESS;
        }

        player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_supply_hint"));
        level().playSound(null, blockPosition(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL,
                0.45f, 0.75f);
        return InteractionResult.SUCCESS;
    }

    private void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private boolean isChaosShard(ItemStack stack) {
        return stack.is(ModItems.CHAOS_SHARD_ALPHA.get())
                || stack.is(ModItems.CHAOS_SHARD_BETA.get())
                || stack.is(ModItems.CHAOS_SHARD_GAMMA.get())
                || stack.is(ModItems.CHAOS_SHARD_DELTA.get())
                || stack.is(ModItems.CHAOS_SHARD_EPSILON.get())
                || stack.is(ModItems.CHAOS_SHARD_ZETA.get())
                || stack.is(ModItems.CHAOS_SHARD_ETA.get())
                || stack.is(ModItems.CHAOS_SHARD_THETA.get())
                || stack.is(ModItems.CHAOS_SHARD_IOTA.get());
    }

    private boolean isNationalFlag(ItemStack stack) {
        return stack.is(ModItems.FLAG_J.get())
                || stack.is(ModItems.FLAG_E.get())
                || stack.is(ModItems.FLAG_U.get())
                || stack.is(ModItems.FLAG_G.get())
                || stack.is(ModItems.FLAG_F.get())
                || stack.is(ModItems.FLAG_I.get())
                || stack.is(ModItems.FLAG_C.get());
    }

    private void playTradeFeedback() {
        level().playSound(null, blockPosition(), SoundEvents.DOLPHIN_PLAY, SoundSource.NEUTRAL,
                0.7f, 0.9f);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.BUBBLE_POP,
                    getX(), getY() + 0.8, getZ(),
                    18, 0.45, 0.35, 0.45, 0.03);
        }
    }
}
