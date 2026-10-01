package com.piranport.dungeon.event;

import com.piranport.PiranPort;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TraceableEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 《副本/00》difficulty_scale 对伤害的缩放。
 *
 * <p>深海舰的炮弹 / 鱼雷伤害写死在射弹构造参数里，不读 ATTACK_DAMAGE 属性，
 * 所以属性缩放对舰炮无效。改为生成时把倍率写进实体 persistentData，
 * 受击时按伤害来源（射弹 owner 或近战本体）回溯倍率。装甲不缩放：
 * 本处理器只放大“攻击方造成的原始伤害”，受击方的装甲/减伤逻辑照常在之后执行。</p>
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public final class DungeonDamageScaleHandler {
    private DungeonDamageScaleHandler() {}

    public static final String DAMAGE_SCALE_KEY = "piranport_dungeon_damage_scale";

    public static void setDamageScale(Entity entity, double scale) {
        if (Math.abs(scale - 1.0) < 1.0e-6) {
            entity.getPersistentData().remove(DAMAGE_SCALE_KEY);
        } else {
            entity.getPersistentData().putDouble(DAMAGE_SCALE_KEY, scale);
        }
    }

    /** 优先级 HIGH：先放大原始伤害，再交给装甲板 / 闪避等减伤处理。 */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) {
            Entity direct = event.getSource().getDirectEntity();
            if (direct instanceof TraceableEntity traceable) attacker = traceable.getOwner();
        }
        if (attacker == null || !attacker.getPersistentData().contains(DAMAGE_SCALE_KEY)) return;
        double scale = attacker.getPersistentData().getDouble(DAMAGE_SCALE_KEY);
        if (scale > 0) event.setAmount((float) (event.getAmount() * scale));
    }
}
