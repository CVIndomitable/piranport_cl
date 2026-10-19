package com.piranport.npc.ai.goal;

import com.piranport.config.ModCommonConfig;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import com.piranport.npc.deepocean.DeepOceanTransportEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Comparator;

/**
 * 《深海/01》§七：副本内深海敌人默认仇恨同实例的护航运输船。
 *
 * <p>玩家攻击过自己，或玩家进入护航仇恨范围时放弃运输船，改由玩家索敌目标接管。
 * 优先级 0 早于子类的玩家索敌，因此运输船不在身边时仍会回落到玩家。</p>
 */
public class EscortTransportTargetGoal extends TargetGoal {

    private static final String INSTANCE_TAG_PREFIX = "dungeon_instance_";

    private final AbstractDeepOceanEntity enemy;
    @Nullable
    private DeepOceanTransportEntity transport;

    public EscortTransportTargetGoal(AbstractDeepOceanEntity enemy) {
        super(enemy, false);
        this.enemy = enemy;
    }

    @Override
    public boolean canUse() {
        String tag = instanceTag();
        if (tag == null) return false;
        transport = findTransport(tag);
        return shouldTargetTransport(transport != null, playerAttackedEnemy(), playerWithinAggro());
    }

    @Override
    public boolean canContinueToUse() {
        return transport != null
                && transport.isAlive()
                && enemy.getTarget() == transport
                && shouldTargetTransport(true, playerAttackedEnemy(), playerWithinAggro());
    }

    @Override
    public void start() {
        enemy.setTarget(transport);
    }

    @Override
    public void stop() {
        if (enemy.getTarget() == transport) {
            enemy.setTarget(null);
        }
        transport = null;
    }

    /** 仅当运输船可用、玩家未攻击敌人且不在护航仇恨范围内时，才仇恨运输船。 */
    static boolean shouldTargetTransport(boolean transportAvailable, boolean playerAttackedEnemy, boolean playerWithinAggro) {
        return transportAvailable && !playerAttackedEnemy && !playerWithinAggro;
    }

    @Nullable
    private String instanceTag() {
        for (String tag : enemy.getTags()) {
            if (tag.startsWith(INSTANCE_TAG_PREFIX)) return tag;
        }
        return null;
    }

    @Nullable
    private DeepOceanTransportEntity findTransport(String instanceTag) {
        double range = enemy.getAttributeValue(Attributes.FOLLOW_RANGE);
        return enemy.level().getEntitiesOfClass(DeepOceanTransportEntity.class,
                        enemy.getBoundingBox().inflate(range),
                        t -> t.isAlive() && t.getTags().contains(instanceTag))
                .stream()
                .min(Comparator.comparingDouble(enemy::distanceToSqr))
                .orElse(null);
    }

    private boolean playerAttackedEnemy() {
        LivingEntity attacker = enemy.getLastHurtByMob();
        return attacker instanceof Player && attacker.isAlive();
    }

    private boolean playerWithinAggro() {
        double range = ModCommonConfig.ESCORT_PLAYER_AGGRO_RANGE.get();
        return enemy.level().getNearestPlayer(TargetingConditions.forCombat().range(range), enemy) != null;
    }
}
