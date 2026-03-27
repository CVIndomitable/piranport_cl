package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.entity.AerialBombEntity;
import com.piranport.entity.AircraftEntity;
import com.piranport.entity.CannonProjectileEntity;
import com.piranport.entity.FloatingTargetEntity;
import com.piranport.entity.TorpedoEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, PiranPort.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<CannonProjectileEntity>>
            CANNON_PROJECTILE = ENTITY_TYPES.register("cannon_projectile",
            () -> EntityType.Builder.<CannonProjectileEntity>of(CannonProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("cannon_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<TorpedoEntity>>
            TORPEDO_ENTITY = ENTITY_TYPES.register("torpedo_entity",
            () -> EntityType.Builder.<TorpedoEntity>of(TorpedoEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.25f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("torpedo_entity"));

    // Phase 19
    public static final DeferredHolder<EntityType<?>, EntityType<AircraftEntity>>
            AIRCRAFT_ENTITY = ENTITY_TYPES.register("aircraft_entity",
            () -> EntityType.Builder.<AircraftEntity>of(AircraftEntity::new, MobCategory.MISC)
                    .sized(1.0f, 0.5f)
                    .clientTrackingRange(16)
                    .updateInterval(3)
                    .build("aircraft_entity"));

    public static final DeferredHolder<EntityType<?>, EntityType<FloatingTargetEntity>>
            FLOATING_TARGET = ENTITY_TYPES.register("floating_target",
            () -> EntityType.Builder.<FloatingTargetEntity>of(FloatingTargetEntity::new, MobCategory.MISC)
                    .sized(0.5f, 1.975f)
                    .clientTrackingRange(10)
                    .build("floating_target"));

    // Phase 21
    public static final DeferredHolder<EntityType<?>, EntityType<AerialBombEntity>>
            AERIAL_BOMB = ENTITY_TYPES.register("aerial_bomb",
            () -> EntityType.Builder.<AerialBombEntity>of(AerialBombEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(8)
                    .updateInterval(5)
                    .build("aerial_bomb"));
}
