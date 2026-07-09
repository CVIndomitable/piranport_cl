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
 * 深海军需官 — a non-combat quartermaster that turns recovered abyssal spoils into battle supplies.
 */
public class DeepOceanQuartermasterEntity extends AbstractDeepOceanEntity {
    private static final int SUPPORT_INTERVAL_TICKS = 90;
    private static final double SUPPORT_RADIUS = 8.0;
    private static final int SUPPORT_TARGET_LIMIT = 4;

    public DeepOceanQuartermasterEntity(EntityType<? extends DeepOceanQuartermasterEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.30)
                .add(Attributes.ARMOR, 5.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new IdleWanderGoal(this, 0.60, 22));
        this.goalSelector.addGoal(5, new FleetAlertGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && tickCount % SUPPORT_INTERVAL_TICKS == 0) {
            resupplyNearbyFleet();
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
            giveOrDrop(player, new ItemStack(ModItems.MEDIUM_HE_SHELL.get(), 8));
            giveOrDrop(player, new ItemStack(ModItems.MEDIUM_AP_SHELL.get(), 8));
            giveOrDrop(player, new ItemStack(ModItems.SMALL_SMOKE_SHELL.get(), 4));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 100, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_quartermaster_report_trade"));
            playSupplyFeedback();
            return InteractionResult.SUCCESS;
        }

        if (held.is(ModItems.FUEL.get())) {
            consume(player, held, 1);
            giveOrDrop(player, new ItemStack(ModItems.AVIATION_FUEL.get(), 2));
            giveOrDrop(player, new ItemStack(ModItems.FIGHTER_AMMO.get(), 8));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 80, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_quartermaster_fuel_trade"));
            playSupplyFeedback();
            return InteractionResult.SUCCESS;
        }

        if (held.is(ModItems.RAW_ALUMINUM.get())) {
            if (!consumeIfEnough(player, held, 6, "message.piranport.deep_ocean_quartermaster_aluminum_short")) {
                return InteractionResult.SUCCESS;
            }
            giveOrDrop(player, new ItemStack(ModItems.REPAIR_KIT.get(), 1));
            giveOrDrop(player, new ItemStack(ModItems.SMALL_HE_SHELL.get(), 8));
            giveOrDrop(player, new ItemStack(Items.IRON_INGOT, 2));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 70, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_quartermaster_aluminum_trade"));
            playSupplyFeedback();
            return InteractionResult.SUCCESS;
        }

        if (held.is(ModItems.EXP_SHELL.get())) {
            consume(player, held, 1);
            giveOrDrop(player, new ItemStack(ModItems.LARGE_GRENADE_SHELL.get(), 3));
            giveOrDrop(player, new ItemStack(ModItems.MEDIUM_FLARE_SHELL.get(), 4));
            giveOrDrop(player, new ItemStack(ModItems.MEDIUM_SMOKE_SHELL.get(), 4));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 80, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_quartermaster_drill_trade"));
            playSupplyFeedback();
            return InteractionResult.SUCCESS;
        }

        if (isNationalFlag(held)) {
            consume(player, held, 1);
            giveOrDrop(player, new ItemStack(ModItems.ABYSSAL_REPORT.get(), 1));
            giveOrDrop(player, new ItemStack(ModItems.AVIATION_FUEL.get(), 4));
            giveOrDrop(player, new ItemStack(ModItems.REPAIR_KIT.get(), 1));
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 20 * 100, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_quartermaster_flag_trade"));
            playSupplyFeedback();
            return InteractionResult.SUCCESS;
        }

        if (isChaosShard(held)) {
            consume(player, held, 1);
            giveOrDrop(player, new ItemStack(ModItems.PORTAL_ACTIVATION_CORE.get(), 1));
            giveOrDrop(player, new ItemStack(ModItems.EXP_SHELL.get(), 2));
            giveOrDrop(player, new ItemStack(ModItems.LARGE_SMOKE_SHELL.get(), 3));
            int supplied = resupplyNearbyFleet();
            player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 20 * 120, 0));
            player.sendSystemMessage(Component.translatable(
                    "message.piranport.deep_ocean_quartermaster_shard_trade", supplied));
            playSupplyFeedback();
            return InteractionResult.SUCCESS;
        }

        player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_quartermaster_hint"));
        level().playSound(null, blockPosition(), SoundEvents.CHEST_OPEN,
                SoundSource.NEUTRAL, 0.38f, 0.70f);
        return InteractionResult.SUCCESS;
    }

    private int resupplyNearbyFleet() {
        int supplied = 0;
        for (AbstractDeepOceanEntity ally : level().getEntitiesOfClass(
                AbstractDeepOceanEntity.class,
                getBoundingBox().inflate(SUPPORT_RADIUS),
                ally -> ally != this && ally.isAlive())) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 8, 0, true, true));
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 5, 0, true, true));
            if (ally.getHealth() < ally.getMaxHealth()) {
                ally.heal(1.0f);
            }
            supplied++;
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.WAX_ON,
                        ally.getX(), ally.getY() + 0.95, ally.getZ(),
                        6, 0.22, 0.24, 0.22, 0.01);
            }
            if (supplied >= SUPPORT_TARGET_LIMIT) {
                break;
            }
        }
        if (supplied > 0) {
            level().playSound(null, blockPosition(), SoundEvents.CHAIN_PLACE,
                    SoundSource.NEUTRAL, 0.42f, 0.74f);
        }
        return supplied;
    }

    private boolean consumeIfEnough(Player player, ItemStack stack, int amount, String shortMessageKey) {
        if (stack.getCount() < amount && !player.getAbilities().instabuild) {
            player.sendSystemMessage(Component.translatable(shortMessageKey));
            playNoSound();
            return false;
        }
        consume(player, stack, amount);
        return true;
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
                SoundSource.NEUTRAL, 0.45f, 0.72f);
    }

    private void playSupplyFeedback() {
        level().playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.NEUTRAL, 0.58f, 0.62f + random.nextFloat() * 0.12f);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    getX(), getY() + 1.0, getZ(),
                    20, 0.45, 0.34, 0.45, 0.02);
        }
    }
}
