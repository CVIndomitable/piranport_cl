package com.piranport.combat;

import com.piranport.PiranPort;
import com.piranport.config.ModProjectilesConfig;
import com.piranport.entity.CannonProjectileEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.List;

/**
 * 让 HE 炮弹的「实体伤害」独立于原版爆炸结算。
 *
 * <p>WHY：原版 {@code level().explode(power)} 里，同一个 power 同时决定半径、方块破坏和
 * 实体伤害，终端无法分别调「伤害倍率」与「半径倍率」。这里在
 * {@link ExplosionEvent.Detonate} 阶段把受影响实体列表清空，让原版只处理方块/粒子/音效；
 * 实体伤害按 {@link HeExplosionMath#entityDamage} 自行结算，并乘上
 * {@code he_entity_damage_multiplier}。
 *
 * <p>只接管「直接来源是本模组 HE 炮弹」的爆炸（VT 近炸的 ammo behavior 也标记为高爆，
 * 因此同样吃倍率）。其他来源（导弹、航弹、鱼雷、原版 TNT 等）完全不受影响。
 *
 * <p>除伤害外还照抄了原版 hurtEntities 的击退与 {@code onExplosionHit}，
 * 避免清空列表后 HE 突然不再推动实体。友伤仍走 {@link FriendlyFireHelper} 现有规则。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public final class HeExplosionHandler {
    private HeExplosionHandler() {}

    // LOWEST：让其他模组（领地/组队保护等）先在 Detonate 里筛掉实体，我们再接管剩余部分。
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide()) return;

        Explosion explosion = event.getExplosion();
        if (!(explosion.getDirectSourceEntity() instanceof CannonProjectileEntity shell)) return;
        if (!shell.isHighExplosive()) return;

        List<Entity> affected = HeExplosionMath.takeOverAffectedEntities(event.getAffectedEntities());
        if (affected.isEmpty()) return;

        double damageMultiplier = ModProjectilesConfig.HE_ENTITY_DAMAGE_MULTIPLIER.get();
        float power = explosion.radius();
        Vec3 center = explosion.center();
        double reach = (double) power * 2.0;
        DamageSource damageSource = Explosion.getDefaultDamageSource(event.getLevel(), shell);
        Entity owner = shell.getOwner();

        for (Entity target : affected) {
            if (target == null || target.ignoreExplosion(explosion)) continue;
            // 友伤规则与原命中路径一致：玩家对玩家等仍放行，内部防呆（自己飞机/女仆等）拦截。
            if (FriendlyFireHelper.shouldBlockHit(target, owner)) continue;

            double distance = Math.sqrt(target.distanceToSqr(center));
            if (distance > reach) continue;
            double normalized = reach > 0.0 ? distance / reach : 1.0;
            float seenPercent = Explosion.getSeenPercent(center, target);

            float amount = HeExplosionMath.entityDamage(power, distance, seenPercent, damageMultiplier);
            if (amount > 0.0f) {
                // 与联装炮齐射一致：同 tick 多发命中不让无敌帧吃掉后续伤害。
                target.invulnerableTime = 0;
                target.hurt(damageSource, amount);
            }
            applyVanillaKnockback(explosion, target, center, normalized, seenPercent);
        }
    }

    /** 原版 {@code Explosion#hurtEntities} 的击退部分（伤害计算器为炮弹默认值，倍率恒为 1）。 */
    private static void applyVanillaKnockback(Explosion explosion, Entity target, Vec3 center,
                                              double normalizedDistance, float seenPercent) {
        double strength = (1.0 - normalizedDistance) * (double) seenPercent;
        if (target instanceof LivingEntity living) {
            strength *= 1.0 - living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
        }
        Vec3 direction = new Vec3(
                target.getX() - center.x,
                (target instanceof PrimedTnt ? target.getY() : target.getEyeY()) - center.y,
                target.getZ() - center.z);
        if (direction.lengthSqr() > 1.0e-8) {
            direction = direction.normalize();
        }
        // 与原版一致，给其他模组留出修改击退的机会。
        Vec3 impulse = EventHooks.getExplosionKnockback(target.level(), explosion, target,
                direction.scale(strength));
        target.setDeltaMovement(target.getDeltaMovement().add(impulse));
        if (target instanceof Player player
                && !player.isSpectator()
                && (!player.isCreative() || !player.getAbilities().flying)) {
            // 原版通过这张表把击退发给客户端，清空实体列表后必须自己补上。
            explosion.getHitPlayers().put(player, impulse);
        }
        target.onExplosionHit(explosion.getDirectSourceEntity());
    }
}
