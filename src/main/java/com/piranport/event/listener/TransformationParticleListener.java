package com.piranport.event.listener;

import com.piranport.PiranPort;
import com.piranport.combat.TransformationManager;
import com.piranport.event.EventBus;
import com.piranport.event.TransformationEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * 变身粒子效果监听器 — 响应变身事件播放粒子特效。
 *
 * <p>职责:
 * <ul>
 *   <li>变身激活时: 播放绿色村民粒子(30个)</li>
 *   <li>变身解除时: 可扩展播放其他粒子效果</li>
 * </ul>
 *
 * <h2>初始化</h2>
 * <p>在模组初始化时调用 {@link #register()} 注册监听器。
 *
 * @since 1.1.0
 */
public final class TransformationParticleListener {

    private TransformationParticleListener() {}

    /**
     * 注册监听器到事件总线。应在模组初始化时调用一次。
     */
    public static void register() {
        EventBus.getInstance().subscribe(TransformationEvent.class,
                TransformationParticleListener::onTransformation);
    }

    private static void onTransformation(TransformationEvent event) {
        if (!event.activated()) {
            return; // 解除变身时不播放粒子
        }

        Player player = event.player();
        if (!(player.level() instanceof ServerLevel sl)) {
            return; // 只在服务端播放粒子
        }

        double px = player.getX();
        double py = player.getY() + 0.5;
        double pz = player.getZ();

        for (int i = 0; i < 30; i++) {
            double ox = (player.getRandom().nextDouble() - 0.5) * 1.5;
            double oy = player.getRandom().nextDouble() * 2.0;
            double oz = (player.getRandom().nextDouble() - 0.5) * 1.5;
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    px + ox, py + oy, pz + oz,
                    1, 0, 0, 0, 0);
        }

        PiranPort.LOGGER.debug("Played transformation particles for player {}",
                player.getName().getString());
    }
}
