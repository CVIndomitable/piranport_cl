package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.entity.CannonProjectileEntity;
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
}
