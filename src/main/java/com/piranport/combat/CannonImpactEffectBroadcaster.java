package com.piranport.combat;

import com.piranport.network.CannonImpactEffectPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/** 服务端广播远距炮弹命中特效。 */
public final class CannonImpactEffectBroadcaster {
    private CannonImpactEffectBroadcaster() {}

    public static void send(Level level, double x, double y, double z,
            float power, CannonImpactEffectPayload.Kind kind) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        double radius = serverLevel.getServer().getPlayerList().getSimulationDistance() * 16.0;
        PacketDistributor.sendToPlayersNear(
                serverLevel,
                null,
                x, y, z,
                radius,
                new CannonImpactEffectPayload(x, y, z, power, kind));
    }
}
