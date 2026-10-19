package com.piranport.npc.ai.goal;

import com.piranport.npc.deepocean.DeepOceanTransportEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 护航运输船直航到设定目标点（《副本/12》：3 米/秒 = 0.15 格/tick，沿直线不寻路）。
 *
 * <p>WHY 用优先级 0 + MOVE 旗标：运行中会抢占跟随领舰与闲逛，让运输船只听护航路线的指挥。
 * WHY 每 tick 直接写水平速度：直航不需要路径，写速度比 {@code moveTo} 更可预测。
 */
public class EscortSailGoal extends Goal {

    static final double SAIL_SPEED = 0.15;
    private static final double VERTICAL_GAIN = 0.2;
    private static final double MAX_VERTICAL_SPEED = 0.05;

    private final DeepOceanTransportEntity transport;

    public EscortSailGoal(DeepOceanTransportEntity transport) {
        this.transport = transport;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return transport.getEscortTarget() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Vec3 target = transport.getEscortTarget();
        if (target == null) return;

        double vx = 0;
        double vz = 0;
        if (!transport.hasArrived()) {
            double dx = target.x - transport.getX();
            double dz = target.z - transport.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 1e-6) {
                vx = dx / len * SAIL_SPEED;
                vz = dz / len * SAIL_SPEED;
            }
        }
        double vy = Math.clamp((target.y - transport.getY()) * VERTICAL_GAIN, -MAX_VERTICAL_SPEED, MAX_VERTICAL_SPEED);
        transport.setDeltaMovement(vx, vy, vz);
    }

    /** 点是否落在箱体的 X/Z 投影内；只看水平面，高度差由垂直速度逐步消除。 */
    public static boolean containsXZ(AABB box, Vec3 point) {
        return point.x >= box.minX && point.x <= box.maxX
                && point.z >= box.minZ && point.z <= box.maxZ;
    }
}
