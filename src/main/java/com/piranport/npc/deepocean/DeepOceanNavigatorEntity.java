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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 深海导航员 — scouting NPC that turns abyssal intel into route tools.
 */
public class DeepOceanNavigatorEntity extends AbstractDeepOceanEntity {
    private static final int SUPPORT_INTERVAL_TICKS = 120;
    private static final double SUPPORT_RADIUS = 9.0;
    private static final int SUPPORT_TARGET_LIMIT = 3;

    public DeepOceanNavigatorEntity(EntityType<? extends DeepOceanNavigatorEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 22.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 22.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.10)
                .add(Attributes.ARMOR, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new IdleWanderGoal(this, 0.75, 28));
        this.goalSelector.addGoal(5, new FleetAlertGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && tickCount % SUPPORT_INTERVAL_TICKS == 0) {
            guideNearbyFleet();
        }
    }

    @Override
    public double getOrbitDistance() {
        return 0.0;
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
            consume(player, held, 1);
            giveOrDrop(player, new ItemStack(Items.MAP, 1));
            int marked = markNearbyAbyssals(player, 18.0, 20 * 12);
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 150, 0));
            player.sendSystemMessage(Component.translatable(
                    "message.piranport.deep_ocean_navigator_report_trade", marked));
            playRouteFeedback(ParticleTypes.ENCHANT);
            return InteractionResult.SUCCESS;
        }

        if (held.is(Items.COMPASS)) {
            consume(player, held, 1);
            giveOrDrop(player, new ItemStack(Items.MAP, 2));
            giveOrDrop(player, new ItemStack(ModItems.ABYSSAL_REPORT.get(), 1));
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 20 * 90, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_navigator_compass_trade"));
            playRouteFeedback(ParticleTypes.END_ROD);
            return InteractionResult.SUCCESS;
        }

        if (held.is(Items.PAPER)) {
            if (held.getCount() < 8 && !player.getAbilities().instabuild) {
                player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_navigator_paper_short"));
                playNoSound();
                return InteractionResult.SUCCESS;
            }
            consume(player, held, 8);
            giveOrDrop(player, new ItemStack(Items.MAP, 1));
            giveOrDrop(player, new ItemStack(Items.COMPASS, 1));
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 20 * 90, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_navigator_paper_trade"));
            playRouteFeedback(ParticleTypes.HAPPY_VILLAGER);
            return InteractionResult.SUCCESS;
        }

        if (held.is(ModItems.AVIATION_FUEL.get())) {
            consume(player, held, 1);
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 90, 0));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 90, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_navigator_fuel_trade"));
            playRouteFeedback(ParticleTypes.CLOUD);
            return InteractionResult.SUCCESS;
        }

        if (isNationalFlag(held)) {
            consume(player, held, 1);
            giveOrDrop(player, new ItemStack(ModItems.ABYSSAL_REPORT.get(), 2));
            giveOrDrop(player, new ItemStack(Items.MAP, 1));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 70, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_navigator_flag_trade"));
            playRouteFeedback(ParticleTypes.FIREWORK);
            return InteractionResult.SUCCESS;
        }

        if (isChaosShard(held)) {
            consume(player, held, 1);
            giveOrDrop(player, new ItemStack(Items.COMPASS, 1));
            giveOrDrop(player, new ItemStack(Items.MAP, 1));
            giveOrDrop(player, new ItemStack(ModItems.EXP_SHELL.get(), 1));
            int marked = markNearbyAbyssals(player, 24.0, 20 * 16);
            player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 20 * 120, 0));
            player.sendSystemMessage(Component.translatable(
                    "message.piranport.deep_ocean_navigator_shard_trade", marked));
            playRouteFeedback(ParticleTypes.REVERSE_PORTAL);
            return InteractionResult.SUCCESS;
        }

        player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_navigator_hint"));
        level().playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.NEUTRAL, 0.45f, 0.95f);
        return InteractionResult.SUCCESS;
    }

    private void guideNearbyFleet() {
        int guided = 0;
        for (AbstractDeepOceanEntity ally : level().getEntitiesOfClass(
                AbstractDeepOceanEntity.class,
                getBoundingBox().inflate(SUPPORT_RADIUS),
                ally -> ally != this && ally.isAlive())) {
            ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 8, 0, true, true));
            guided++;
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        ally.getX(), ally.getY() + 1.0, ally.getZ(),
                        5, 0.20, 0.22, 0.20, 0.01);
            }
            if (guided >= SUPPORT_TARGET_LIMIT) {
                break;
            }
        }
    }

    private int markNearbyAbyssals(Player player, double radius, int durationTicks) {
        int marked = 0;
        for (AbstractDeepOceanEntity entity : level().getEntitiesOfClass(
                AbstractDeepOceanEntity.class,
                player.getBoundingBox().inflate(radius),
                entity -> entity != this && entity.isAlive())) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, durationTicks, 0, true, true));
            marked++;
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        entity.getX(), entity.getY() + 1.15, entity.getZ(),
                        8, 0.24, 0.30, 0.24, 0.02);
            }
        }
        return marked;
    }

    private void consume(Player player, ItemStack stack, int amount) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(amount);
        }
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

    private void playNoSound() {
        level().playSound(null, blockPosition(), SoundEvents.VILLAGER_NO,
                SoundSource.NEUTRAL, 0.45f, 0.82f);
    }

    private void playRouteFeedback(net.minecraft.core.particles.ParticleOptions particle) {
        level().playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.NEUTRAL, 0.55f, 0.82f + random.nextFloat() * 0.18f);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle,
                    getX(), getY() + 1.05, getZ(),
                    20, 0.45, 0.36, 0.45, 0.02);
        }
    }
}
