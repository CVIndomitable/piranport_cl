package com.piranport.aviation.combat;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** 水平轰炸机的每架次运行时状态（进入 ATTACKING 时重置）。 */
public final class LevelBombState {
    public boolean dropped;
    @Nullable public Vec3 runDirection;
    @Nullable public Vec3 dropPoint;
    public int runTicks;
    public int abortTicks;
    public boolean rearApproachDone;

    public void reset() {
        dropped = false;
        runDirection = null;
        dropPoint = null;
        runTicks = 0;
        abortTicks = 0;
        rearApproachDone = false;
    }
}
