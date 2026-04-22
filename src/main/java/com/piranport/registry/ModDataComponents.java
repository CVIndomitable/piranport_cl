package com.piranport.registry;

import com.mojang.serialization.Codec;
import com.piranport.PiranPort;
import com.piranport.component.AircraftInfo;
import com.piranport.component.FlightGroupData;
import com.piranport.component.FuelData;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.PlaceableInfo;
import com.piranport.component.SlotCooldowns;
import com.piranport.component.WeaponCategory;
import com.piranport.component.WeaponCooldown;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, PiranPort.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>>
            SHIP_CORE_CONTENTS = DATA_COMPONENTS.register("ship_core_contents",
            () -> DataComponentType.<ItemContainerContents>builder()
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>>
            SHIP_CORE_TRANSFORMED = DATA_COMPONENTS.register("ship_core_transformed",
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>>
            SHIP_CORE_WEAPON_INDEX = DATA_COMPONENTS.register("ship_core_weapon_index",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PlaceableInfo>>
            PLACEABLE_INFO = DATA_COMPONENTS.register("placeable_info",
            () -> DataComponentType.<PlaceableInfo>builder()
                    .persistent(PlaceableInfo.CODEC)
                    .networkSynchronized(PlaceableInfo.STREAM_CODEC)
                    .build());

    // ===== v0.4.0 Aviation DataComponents =====

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AircraftInfo>>
            AIRCRAFT_INFO = DATA_COMPONENTS.register("aircraft_info",
            () -> DataComponentType.<AircraftInfo>builder()
                    .persistent(AircraftInfo.CODEC)
                    .networkSynchronized(AircraftInfo.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FlightGroupData>>
            FLIGHT_GROUP_DATA = DATA_COMPONENTS.register("flight_group_data",
            () -> DataComponentType.<FlightGroupData>builder()
                    .persistent(FlightGroupData.CODEC)
                    .networkSynchronized(FlightGroupData.STREAM_CODEC)
                    .build());

    // ===== v0.0.7 Ship Config DataComponents =====

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>>
            SHIP_AUTO_LAUNCH = DATA_COMPONENTS.register("ship_auto_launch",
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    // ===== 武器种类标签 =====

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WeaponCategory>>
            WEAPON_CATEGORY = DATA_COMPONENTS.register("weapon_category",
            () -> DataComponentType.<WeaponCategory>builder()
                    .persistent(WeaponCategory.CODEC)
                    .networkSynchronized(WeaponCategory.STREAM_CODEC)
                    .build());

    // ===== 分槽冷却 =====

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SlotCooldowns>>
            SLOT_COOLDOWNS = DATA_COMPONENTS.register("slot_cooldowns",
            () -> DataComponentType.<SlotCooldowns>builder()
                    .persistent(SlotCooldowns.CODEC)
                    .networkSynchronized(SlotCooldowns.STREAM_CODEC)
                    .build());

    // ===== 武器冷却（无GUI模式，直接存在武器物品上） =====

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WeaponCooldown>>
            WEAPON_COOLDOWN = DATA_COMPONENTS.register("weapon_cooldown",
            () -> DataComponentType.<WeaponCooldown>builder()
                    .persistent(WeaponCooldown.CODEC)
                    .networkSynchronized(WeaponCooldown.STREAM_CODEC)
                    .build());

    // ===== 手动装填弹药 =====

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LoadedAmmo>>
            LOADED_AMMO = DATA_COMPONENTS.register("loaded_ammo",
            () -> DataComponentType.<LoadedAmmo>builder()
                    .persistent(LoadedAmmo.CODEC)
                    .networkSynchronized(LoadedAmmo.STREAM_CODEC)
                    .build());

    // ===== 无GUI模式舰装核心储存护甲板 =====

    /** Stores ArmorPlateItems slotted into the ship core (no-GUI mode only). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>>
            SHIP_CORE_ARMOR = DATA_COMPONENTS.register("ship_core_armor",
            () -> DataComponentType.<ItemContainerContents>builder()
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                    .build());

    // ===== 舰装核心燃料库 =====

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FuelData>>
            SHIP_CORE_FUEL = DATA_COMPONENTS.register("ship_core_fuel",
            () -> DataComponentType.<FuelData>builder()
                    .persistent(FuelData.CODEC)
                    .networkSynchronized(FuelData.STREAM_CODEC)
                    .build());

}
