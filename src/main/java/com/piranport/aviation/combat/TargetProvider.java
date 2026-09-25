package com.piranport.aviation.combat;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.entity.AircraftEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** 服务端目标解析适配器。实现必须无状态。 */
@FunctionalInterface
public interface TargetProvider {
    @Nullable LivingEntity findTarget(TargetContext context);

    record TargetContext(AircraftEntity aircraft, @Nullable Player owner, AircraftDefinition definition) {
        public TargetContext {
            if (aircraft == null || definition == null) {
                throw new NullPointerException("aircraft and definition are required");
            }
        }
    }
}
