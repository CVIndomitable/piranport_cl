package com.piranport.aviation.combat;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** 俯冲轰炸机的每架次运行时状态（进入 ATTACKING 时重置）。 */
public final class DiveBombState {
    public boolean committed;
    @Nullable public Vec3 target;

    public void reset() {
        committed = false;
        target = null;
    }
}
