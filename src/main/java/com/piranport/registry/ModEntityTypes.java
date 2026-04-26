package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.entity.AerialBombEntity;
import com.piranport.entity.AircraftDropEntity;
import com.piranport.entity.AircraftEntity;
import com.piranport.entity.BulletEntity;
import com.piranport.entity.CannonProjectileEntity;
import com.piranport.entity.FlareProjectileEntity;
import com.piranport.entity.GungnirEntity;
import com.piranport.entity.RailgunProjectileEntity;
import com.piranport.entity.SanshikiPelletEntity;
import com.piranport.entity.DepthChargeEntity;
import com.piranport.entity.MissileEntity;
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
                    .build("piranport:cannon_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<TorpedoEntity>>
            TORPEDO_ENTITY = ENTITY_TYPES.register("torpedo_entity",
            () -> EntityType.Builder.<TorpedoEntity>of(TorpedoEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.25f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("piranport:torpedo_entity"));

    // Phase 19
    public static final DeferredHolder<EntityType<?>, EntityType<AircraftEntity>>
            AIRCRAFT_ENTITY = ENTITY_TYPES.register("aircraft_entity",
            () -> EntityType.Builder.<AircraftEntity>of(AircraftEntity::new, MobCategory.MISC)
                    .sized(1.0f, 0.5f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("piranport:aircraft_entity"));

    public static final DeferredHolder<EntityType<?>, EntityType<AircraftDropEntity>>
            AIRCRAFT_DROP = ENTITY_TYPES.register("aircraft_drop",
            () -> EntityType.Builder.<AircraftDropEntity>of(AircraftDropEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(6)
                    .updateInterval(20)
                    .build("piranport:aircraft_drop"));

    // Phase 21
    public static final DeferredHolder<EntityType<?>, EntityType<AerialBombEntity>>
            AERIAL_BOMB = ENTITY_TYPES.register("aerial_bomb",
            () -> EntityType.Builder.<AerialBombEntity>of(AerialBombEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(8)
                    .updateInterval(5)
                    .build("piranport:aerial_bomb"));

    public static final DeferredHolder<EntityType<?>, EntityType<DepthChargeEntity>>
            DEPTH_CHARGE = ENTITY_TYPES.register("depth_charge",
            () -> EntityType.Builder.<DepthChargeEntity>of(DepthChargeEntity::new, MobCategory.MISC)
                    .sized(0.35f, 0.35f)
                    .clientTrackingRange(8)
                    .updateInterval(5)
                    .build("piranport:depth_charge"));

    public static final DeferredHolder<EntityType<?>, EntityType<BulletEntity>>
            BULLET = ENTITY_TYPES.register("bullet",
            () -> EntityType.Builder.<BulletEntity>of(BulletEntity::new, MobCategory.MISC)
                    .sized(0.15f, 0.15f)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("piranport:bullet"));

    // Sanshiki (Type 3) pellet
    public static final DeferredHolder<EntityType<?>, EntityType<SanshikiPelletEntity>>
            SANSHIKI_PELLET = ENTITY_TYPES.register("sanshiki_pellet",
            () -> EntityType.Builder.<SanshikiPelletEntity>of(SanshikiPelletEntity::new, MobCategory.MISC)
                    .sized(0.15f, 0.15f)
                    .clientTrackingRange(4)
                    .updateInterval(5)
                    .build("piranport:sanshiki_pellet"));

    // Missile / Rocket
    public static final DeferredHolder<EntityType<?>, EntityType<MissileEntity>>
            MISSILE_ENTITY = ENTITY_TYPES.register("missile_entity",
            () -> EntityType.Builder.<MissileEntity>of(MissileEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("piranport:missile_entity"));

    // Flare projectile
    public static final DeferredHolder<EntityType<?>, EntityType<FlareProjectileEntity>>
            FLARE_PROJECTILE = ENTITY_TYPES.register("flare_projectile",
            () -> EntityType.Builder.<FlareProjectileEntity>of(FlareProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(8)
                    .updateInterval(5)
                    .build("piranport:flare_projectile"));

    // Railgun projectile
    public static final DeferredHolder<EntityType<?>, EntityType<RailgunProjectileEntity>>
            RAILGUN_PROJECTILE = ENTITY_TYPES.register("railgun_projectile",
            () -> EntityType.Builder.<RailgunProjectileEntity>of(RailgunProjectileEntity::new, MobCategory.MISC)
                    .sized(0.15f, 0.15f)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("piranport:railgun_projectile"));

    // Gungnir thrown spear
    public static final DeferredHolder<EntityType<?>, EntityType<GungnirEntity>>
            GUNGNIR = ENTITY_TYPES.register("gungnir",
            () -> EntityType.Builder.<GungnirEntity>of(GungnirEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("piranport:gungnir"));

}
