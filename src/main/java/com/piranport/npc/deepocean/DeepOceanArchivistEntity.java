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
 * 深海档案员 — non-combat research NPC for abyssal lore, mapping and shard analysis.
 */
public class DeepOceanArchivistEntity extends AbstractDeepOceanEntity {

    public DeepOceanArchivistEntity(EntityType<? extends DeepOceanArchivistEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FOLLOW_RANGE, 18.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.15)
                .add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new IdleWanderGoal(this, 0.65, 24));
        this.goalSelector.addGoal(5, new FleetAlertGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && tickCount % 120 == 0 && level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    getX(), getY() + 1.0, getZ(),
                    8, 0.35, 0.22, 0.35, 0.01);
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
            giveOrDrop(player, new ItemStack(ModItems.EXP_SHELL.get(), 1));
            giveOrDrop(player, new ItemStack(Items.MAP, 1));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 120, 0));
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 20 * 120, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_archivist_report_trade"));
            playResearchFeedback();
            return InteractionResult.SUCCESS;
        }

        if (isChaosShard(held)) {
            consume(player, held, 1);
            giveOrDrop(player, new ItemStack(ModItems.ABYSSAL_REPORT.get(), 2));
            giveOrDrop(player, new ItemStack(ModItems.RAW_ALUMINUM.get(), 6));
            player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 20 * 120, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_archivist_shard_trade"));
            playResearchFeedback();
            return InteractionResult.SUCCESS;
        }

        if (held.is(Items.PAPER)) {
            if (held.getCount() < 6 && !player.getAbilities().instabuild) {
                player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_archivist_paper_short"));
                playNoSound();
                return InteractionResult.SUCCESS;
            }
            consume(player, held, 6);
            giveOrDrop(player, new ItemStack(ModItems.ABYSSAL_REPORT.get(), 1));
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 20 * 60, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_archivist_paper_trade"));
            playResearchFeedback();
            return InteractionResult.SUCCESS;
        }

        if (isNationalFlag(held)) {
            consume(player, held, 1);
            giveOrDrop(player, randomShard());
            giveOrDrop(player, new ItemStack(ModItems.EXP_SHELL.get(), 1));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 80, 0));
            player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_archivist_flag_trade"));
            playResearchFeedback();
            return InteractionResult.SUCCESS;
        }

        player.sendSystemMessage(Component.translatable("message.piranport.deep_ocean_archivist_hint"));
        level().playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.NEUTRAL, 0.45f, 0.62f);
        return InteractionResult.SUCCESS;
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

    private ItemStack randomShard() {
        return switch (random.nextInt(9)) {
            case 0 -> new ItemStack(ModItems.CHAOS_SHARD_ALPHA.get());
            case 1 -> new ItemStack(ModItems.CHAOS_SHARD_BETA.get());
            case 2 -> new ItemStack(ModItems.CHAOS_SHARD_GAMMA.get());
            case 3 -> new ItemStack(ModItems.CHAOS_SHARD_DELTA.get());
            case 4 -> new ItemStack(ModItems.CHAOS_SHARD_EPSILON.get());
            case 5 -> new ItemStack(ModItems.CHAOS_SHARD_ZETA.get());
            case 6 -> new ItemStack(ModItems.CHAOS_SHARD_ETA.get());
            case 7 -> new ItemStack(ModItems.CHAOS_SHARD_THETA.get());
            default -> new ItemStack(ModItems.CHAOS_SHARD_IOTA.get());
        };
    }

    private void playNoSound() {
        level().playSound(null, blockPosition(), SoundEvents.VILLAGER_NO,
                SoundSource.NEUTRAL, 0.45f, 0.70f);
    }

    private void playResearchFeedback() {
        level().playSound(null, blockPosition(), SoundEvents.AMETHYST_CLUSTER_HIT,
                SoundSource.NEUTRAL, 0.65f, 0.85f);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    getX(), getY() + 0.95, getZ(),
                    24, 0.45, 0.30, 0.45, 0.02);
        }
    }
}
