package com.piranport.item;

import com.piranport.combat.AASilenceManager;
import com.piranport.combat.TransformationManager;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.entity.AircraftEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;

/** 强化槽中的自动近防炮；只有已装入核心的部件才提供独立自动火力。 */
public class AutoCIWSItem extends Item {
    public static final double DETECT_RADIUS = 16.0;
    public static final float DAMAGE = 1.5f;
    public static final int FIRE_INTERVAL = 20;

    private final float caliberDamage;
    private final ModEquipmentConfig.CIWSConfig config;

    public AutoCIWSItem(Properties properties, float caliberDamage,
                        ModEquipmentConfig.CIWSConfig config) {
        super(properties);
        this.caliberDamage = caliberDamage;
        this.config = config;
    }

    public float getCaliberDamage() { return caliberDamage; }
    public int getWeight() { return config.weight().get(); }

    /** 敌机包括敌对生物、深海自主飞机和非友军玩家飞机；不拦截普通箭矢。 */
    public static boolean isHostileAircraft(Player player, Entity target) {
        if (!target.isAlive() || target == player || target.isAlliedTo(player)) return false;
        if (target instanceof AircraftEntity aircraft) {
            if (aircraft.getOwnerUUID() != null && aircraft.getOwnerUUID().equals(player.getUUID())) return false;
            Player owner = aircraft.getOwner();
            if (owner != null) return player.canHarmPlayer(owner) && !player.isAlliedTo(owner);
            return aircraft.isAutonomous();
        }
        return target instanceof LivingEntity living && target instanceof Enemy && (living instanceof FlyingMob
                || living instanceof Phantom || living instanceof Vex);
    }

    public static void tickAutoCIWS(Player player, boolean autoFireEnabled) {
        if (player == null || !autoFireEnabled || player.level().isClientSide()
                || AASilenceManager.isSilenced(player)) return;
        ItemStack core = TransformationManager.findTransformedCore(player);
        for (ItemStack enhancement : TransformationManager.getCoreStoredContents(core)) {
            if (!(enhancement.getItem() instanceof AutoCIWSItem ciws)) continue;
            if (player.tickCount % ciws.config.interval().get() != 0) continue;
            double range = ciws.config.range().get();
            Entity target = player.level().getEntitiesOfClass(Entity.class,
                    player.getBoundingBox().inflate(range),
                    candidate -> isHostileAircraft(player, candidate)
                            && player.distanceToSqr(candidate) <= range * range
                            && player.hasLineOfSight(candidate)).stream()
                    .min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
            if (target == null) continue;
            // 舰载机继承 Entity 并自行处理耐久；不能在已选中飞机后再用 LivingEntity 过滤掉。
            target.hurt(player.damageSources().mobAttack(player),
                    DAMAGE * ciws.caliberDamage * ciws.config.barrels().get());
        }
    }
}
