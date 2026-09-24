package com.piranport.npc.deepocean;

import com.piranport.registry.ModItems;
import com.piranport.npc.ai.goal.CannonAttackGoal;
import com.piranport.npc.ai.goal.FleetAlertGoal;
import com.piranport.npc.ai.goal.IdleWanderGoal;
import com.piranport.npc.ai.goal.OrbitTargetGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 深海旗舰/Boss — Highest tier, can use torpedoes and launch aircraft.
 * HP:200 Armor:12 Speed:0.15 Detection:64 Orbit:48
 * Data-driven from boss.json; hardcoded fallback matches策划 values.
 */
public class DeepOceanBossEntity extends AbstractDeepOceanEntity {

    public DeepOceanBossEntity(EntityType<? extends DeepOceanBossEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.MOVEMENT_SPEED, 0.15)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ARMOR, 12.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new OrbitTargetGoal(this, 0.06));
        this.goalSelector.addGoal(2, new CannonAttackGoal(this));
        this.goalSelector.addGoal(5, new IdleWanderGoal(this, 0.4, 32));
        this.goalSelector.addGoal(4, new FleetAlertGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, AbstractDeepOceanEntity.class));
    }

    @Override public double getOrbitDistance() { return 48.0; }
    @Override public int getFireInterval() { return 60; }
    @Override public float getShellDamage() { return 15.0f; }
    @Override public float getExplosionPower() { return 4.0f; }
    @Override public int getTrackingIntervalMin() { return 2; }
    @Override public int getTrackingIntervalMax() { return 4; }
    @Override public boolean canUseTorpedoes() { return true; }
    @Override public float getTorpedoDamage() { return 20.0f; }
    @Override public boolean canLaunchAircraft() { return true; }
    @Override public int getMaxAircraft() { return 6; }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        // Boss drops: high-tier resources + chance for a key fragment + chance for MK23
        spawnAtLocation(new ItemStack(Items.IRON_INGOT, 5 + random.nextInt(4)));
        spawnAtLocation(new ItemStack(Items.GUNPOWDER, 4 + random.nextInt(3)));
        spawnAtLocation(new ItemStack(ModItems.FUEL.get(), 3 + random.nextInt(3)));
        spawnAtLocation(new ItemStack(ModItems.RAW_ALUMINUM.get(), 4 + random.nextInt(5)));
        spawnKeyFragment(level);
        if (random.nextFloat() < 0.15f) {
            spawnAtLocation(new ItemStack(ModItems.ELITE_DAMAGE_CONTROL.get(), 1));
        }
    }
}
