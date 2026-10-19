package com.piranport.dungeon.weather;

import com.piranport.PiranPort;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 大凤「晴空万里」的航空减伤。
 *
 * <p>航空炸弹与航空鱼雷的伤害源和舰炮、舰载鱼雷、战斗机子弹共用 thrown 等类型，且直接来源可能是飞机本体，
 * 无法从伤害源本身区分。因此在投弹/投雷的命中点用 {@link #hurt} 包一层标记，受击事件里读取该标记。</p>
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public final class WeatherDamageHandler {

    private static final ThreadLocal<Boolean> AIR_ORDNANCE = ThreadLocal.withInitial(() -> false);

    private WeatherDamageHandler() {}

    public static boolean hurt(Entity target, DamageSource source, float amount, boolean airOrdnance) {
        boolean previous = AIR_ORDNANCE.get();
        AIR_ORDNANCE.set(airOrdnance);
        try {
            return target.hurt(source, amount);
        } finally {
            AIR_ORDNANCE.set(previous);
        }
    }

    /** 优先级 HIGH：与副本难度缩放同级，减伤后再交给装甲等后续结算。 */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof AbstractDeepOceanEntity victim)) return;
        if (!AIR_ORDNANCE.get()) return;
        if (!(victim.level() instanceof ServerLevel level)) return;
        float factor = WeatherRules.airOrdnanceDamageFactor(true,
                WeatherRuntime.isProtectedByTaihou(level, victim));
        if (factor != 1.0f) event.setAmount(event.getAmount() * factor);
    }
}
