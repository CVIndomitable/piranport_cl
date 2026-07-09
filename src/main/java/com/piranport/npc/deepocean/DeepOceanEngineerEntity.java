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
 * 深海工程员 — repair and construction NPC for abyssal-world utility trades.
 */
public class DeepOceanEngineerEntity extends AbstractDeepOceanEntity {
    private static final int SUPPORT_INTERVAL_TICKS = 100;
    private static final double SUPPORT_RADIUS = 7.0;
    private static final int SUPPORT_TARGET_LIMIT = 3;

    public DeepOceanEngineerEntity(EntityType<? extends DeepOceanEngineerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 28.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 18.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.25)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new IdleWanderGoal(this, 0.58, 20));
        this.goalSelector.addGoal(5, new FleetAlertGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && tickCount % SUPPORT_INTERVAL_TICKS == 0) {
            reinforceNearbyFleet(1.5f, false);
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
        if (held.is(ModItems.RAW_ALUMINUM.get())) {
            if (!consumeIfEnough(player, held, 8, "message.piranport.deep_ocean_engineer_aluminum_short")) {
                return InteractionResult.SUCCESS;
            }
            giveOrDrop(player, new ItemStack(ModItems.REPAIR_KIT.get(), 1));
            giveOrDrop(player, new ItemStack(Items.IRON_INGOT, 2));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 70, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_engineer_aluminum_trade"));
            playWorkFeedback(ParticleTypes.ELECTRIC_SPARK);
            return InteractionResult.SUCCESS;
        }

        if (held.is(ModItems.REPAIR_KIT.get())) {
            consume(player, held, 1);
            int repaired = reinforceNearbyFleet(8.0f, true);
            giveOrDrop(player, new ItemStack(ModItems.EXP_SHELL.get(), 1));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 40, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_engineer_repair_trade", repaired));
            playWorkFeedback(ParticleTypes.WAX_ON);
            return InteractionResult.SUCCESS;
        }

        if (held.is(ModItems.ABYSSAL_REPORT.get())) {
            consume(player, held, 1);
            giveOrDrop(player, new ItemStack(ModItems.ABYSSAL_SEEP.get(), 1));
            giveOrDrop(player, new ItemStack(ModItems.ALUMINUM_INGOT.get(), 2));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 20 * 90, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_engineer_report_trade"));
            playWorkFeedback(ParticleTypes.ENCHANT);
            return InteractionResult.SUCCESS;
        }

        if (held.is(ModItems.FUEL.get())) {
            consume(player, held, 1);
            giveOrDrop(player, new ItemStack(ModItems.RAW_ALUMINUM.get(), 5));
            giveOrDrop(player, new ItemStack(Items.REDSTONE, 2));
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 20 * 90, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_engineer_fuel_trade"));
            playWorkFeedback(ParticleTypes.FLAME);
            return InteractionResult.SUCCESS;
        }

        if (isChaosShard(held)) {
            consume(player, held, 1);
            giveOrDrop(player, new ItemStack(ModItems.REPAIR_KIT.get(), 1));
            giveOrDrop(player, new ItemStack(ModItems.RAW_ALUMINUM.get(), 6));
            player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 20 * 120, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_engineer_shard_trade"));
            playWorkFeedback(ParticleTypes.REVERSE_PORTAL);
            return InteractionResult.SUCCESS;
        }

        player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_engineer_hint"));
        level().playSound(null, blockPosition(), SoundEvents.ANVIL_LAND,
                SoundSource.NEUTRAL, 0.32f, 0.68f);
        return InteractionResult.SUCCESS;
    }

    private int reinforceNearbyFleet(float healAmount, boolean widePulse) {
        int reinforced = 0;
        double radius = widePulse ? SUPPORT_RADIUS + 3.0 : SUPPORT_RADIUS;
        for (AbstractDeepOceanEntity ally : level().getEntitiesOfClass(
                AbstractDeepOceanEntity.class,
                getBoundingBox().inflate(radius),
                ally -> ally != this && ally.isAlive())) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 7, 0, true, true));
            if (ally.getHealth() < ally.getMaxHealth()) {
                ally.heal(healAmount);
            }
            reinforced++;
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        ally.getX(), ally.getY() + 0.85, ally.getZ(),
                        7, 0.22, 0.25, 0.22, 0.02);
            }
            if (reinforced >= SUPPORT_TARGET_LIMIT) {
                break;
            }
        }

        if (reinforced > 0) {
            level().playSound(null, blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(),
                    SoundSource.NEUTRAL, 0.45f, 0.82f);
        }
        return reinforced;
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

    private void playNoSound() {
        level().playSound(null, blockPosition(), SoundEvents.VILLAGER_NO,
                SoundSource.NEUTRAL, 0.45f, 0.72f);
    }

    private void playWorkFeedback(net.minecraft.core.particles.ParticleOptions particle) {
        level().playSound(null, blockPosition(), SoundEvents.ANVIL_USE,
                SoundSource.NEUTRAL, 0.58f, 0.74f);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle,
                    getX(), getY() + 0.9, getZ(),
                    22, 0.42, 0.35, 0.42, 0.03);
        }
    }
}
