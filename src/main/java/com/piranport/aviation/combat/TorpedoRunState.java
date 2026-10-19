package com.piranport.aviation.combat;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** 鱼雷机的每架次运行时状态（进入 ATTACKING 时重置）。 */
public final class TorpedoRunState {
    /** 非 null 表示已进入低空平飞段。 */
    @Nullable public Vec3 runStart;
    public int runTicks;
    /** 平飞段每 tick 解算出的预测撞击点；接近段为 null。 */
    @Nullable public Vec3 aimPoint;

    public void reset() {
        runStart = null;
        runTicks = 0;
        aimPoint = null;
    }
}
