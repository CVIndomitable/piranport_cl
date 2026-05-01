package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.entity.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, PiranPort.MOD_ID);

    public static final RegistryObject<EntityType<CannonProjectileEntity>> CANNON_PROJECTILE =
            ENTITY_TYPES.register("cannon_projectile",
                    () -> EntityType.Builder.<CannonProjectileEntity>of(CannonProjectileEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("piranport:cannon_projectile"));

    public static final RegistryObject<EntityType<TorpedoEntity>> TORPEDO_ENTITY =
            ENTITY_TYPES.register("torpedo_entity",
                    () -> EntityType.Builder.<TorpedoEntity>of(TorpedoEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.25f)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build("piranport:torpedo_entity"));

    public static final RegistryObject<EntityType<AircraftEntity>> AIRCRAFT_ENTITY =
            ENTITY_TYPES.register("aircraft_entity",
                    () -> EntityType.Builder.<AircraftEntity>of(AircraftEntity::new, MobCategory.MISC)
                            .sized(1.0f, 0.5f)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("piranport:aircraft_entity"));

    public static final RegistryObject<EntityType<AircraftDropEntity>> AIRCRAFT_DROP =
            ENTITY_TYPES.register("aircraft_drop",
                    () -> EntityType.Builder.<AircraftDropEntity>of(AircraftDropEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(6)
                            .updateInterval(20)
                            .build("piranport:aircraft_drop"));

    public static final RegistryObject<EntityType<AerialBombEntity>> AERIAL_BOMB =
            ENTITY_TYPES.register("aerial_bomb",
                    () -> EntityType.Builder.<AerialBombEntity>of(AerialBombEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(8)
                            .updateInterval(5)
                            .build("piranport:aerial_bomb"));

    public static final RegistryObject<EntityType<DepthChargeEntity>> DEPTH_CHARGE =
            ENTITY_TYPES.register("depth_charge",
                    () -> EntityType.Builder.<DepthChargeEntity>of(DepthChargeEntity::new, MobCategory.MISC)
                            .sized(0.35f, 0.35f)
                            .clientTrackingRange(8)
                            .updateInterval(5)
                            .build("piranport:depth_charge"));

    public static final RegistryObject<EntityType<BulletEntity>> BULLET =
            ENTITY_TYPES.register("bullet",
                    () -> EntityType.Builder.<BulletEntity>of(BulletEntity::new, MobCategory.MISC)
                            .sized(0.15f, 0.15f)
                            .clientTrackingRange(8)
                            .updateInterval(2)
                            .build("piranport:bullet"));

    public static final RegistryObject<EntityType<SanshikiPelletEntity>> SANSHIKI_PELLET =
            ENTITY_TYPES.register("sanshiki_pellet",
                    () -> EntityType.Builder.<SanshikiPelletEntity>of(SanshikiPelletEntity::new, MobCategory.MISC)
                            .sized(0.15f, 0.15f)
                            .clientTrackingRange(4)
                            .updateInterval(5)
                            .build("piranport:sanshiki_pellet"));

    public static final RegistryObject<EntityType<MissileEntity>> MISSILE_ENTITY =
            ENTITY_TYPES.register("missile_entity",
                    () -> EntityType.Builder.<MissileEntity>of(MissileEntity::new, MobCategory.MISC)
                            .sized(0.3f, 0.3f)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build("piranport:missile_entity"));

    public static final RegistryObject<EntityType<FlareProjectileEntity>> FLARE_PROJECTILE =
            ENTITY_TYPES.register("flare_projectile",
                    () -> EntityType.Builder.<FlareProjectileEntity>of(FlareProjectileEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(8)
                            .updateInterval(5)
                            .build("piranport:flare_projectile"));

    public static final RegistryObject<EntityType<RailgunProjectileEntity>> RAILGUN_PROJECTILE =
            ENTITY_TYPES.register("railgun_projectile",
                    () -> EntityType.Builder.<RailgunProjectileEntity>of(RailgunProjectileEntity::new, MobCategory.MISC)
                            .sized(0.15f, 0.15f)
                            .clientTrackingRange(8)
                            .updateInterval(2)
                            .build("piranport:railgun_projectile"));

    public static final RegistryObject<EntityType<GungnirEntity>> GUNGNIR =
            ENTITY_TYPES.register("gungnir",
                    () -> EntityType.Builder.<GungnirEntity>of(GungnirEntity::new, MobCategory.MISC)
                            .sized(0.3f, 0.3f)
                            .clientTrackingRange(8)
                            .updateInterval(2)
                            .build("piranport:gungnir"));

    private ModEntityTypes() {}
}
