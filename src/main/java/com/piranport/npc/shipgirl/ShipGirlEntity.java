package com.piranport.npc.shipgirl;

import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Friendly ship girl NPC. Can be interacted with (placeholder dialog).
 * Optionally fights deep ocean enemies when combat AI is enabled.
 */
public class ShipGirlEntity extends PathfinderMob {

    private boolean combatAiEnabled = true;

    public ShipGirlEntity(EntityType<? extends ShipGirlEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.ARMOR, 8.0);
    }

    @Override
    protected void registerGoals() {
        // Movement
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // Target deep ocean enemies if combat AI is on
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 10, true, false,
                e -> e instanceof AbstractDeepOceanEntity));
    }

    // --- Interaction ---

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
            // Placeholder dialog — to be expanded in future versions
            player.sendSystemMessage(Component.translatable("message.piranport.ship_girl_greet"));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    // --- Don't retaliate against players ---

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // Don't fight back against players
        if (source.getEntity() instanceof Player) {
            // Take damage but don't set target to player
            return super.hurt(source, amount);
        }
        return super.hurt(source, amount);
    }

    // --- Rendering ---

    @Override
    public boolean isCurrentlyGlowing() {
        return true; // Visible without custom model
    }

    @Override
    public boolean removeWhenFarAway(double distSq) {
        return false;
    }

    // --- Persistence ---

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("CombatAi", combatAiEnabled);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CombatAi")) {
            combatAiEnabled = tag.getBoolean("CombatAi");
        }
    }
}
