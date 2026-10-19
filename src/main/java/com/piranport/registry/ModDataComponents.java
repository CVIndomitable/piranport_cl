package com.piranport.registry;

import com.mojang.serialization.Codec;
import com.piranport.PiranPort;
import com.piranport.component.AircraftState;
import com.piranport.component.CreativeAmmoSelection;
import com.piranport.component.CustomCoreConfig;
import com.piranport.component.FuelData;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.PlaceableInfo;
import com.piranport.component.SelectedAmmoType;
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

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>>
            SHIP_CORE_TRANSFORMED = DATA_COMPONENTS.register("ship_core_transformed",
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PlaceableInfo>>
            PLACEABLE_INFO = DATA_COMPONENTS.register("placeable_info",
            () -> DataComponentType.<PlaceableInfo>builder()
                    .persistent(PlaceableInfo.CODEC)
                    .networkSynchronized(PlaceableInfo.STREAM_CODEC)
                    .build());

    // ===== v0.4.0 Aviation DataComponents =====

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AircraftState>>
            AIRCRAFT_STATE = DATA_COMPONENTS.register("aircraft_state",
            () -> DataComponentType.<AircraftState>builder()
                    .persistent(AircraftState.CODEC)
                    .networkSynchronized(AircraftState.STREAM_CODEC)
                    .build());

    // ===== v0.0.7 Ship Config DataComponents =====

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>>
            SHIP_AUTO_LAUNCH = DATA_COMPONENTS.register("ship_auto_launch",
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    // ===== 自动模式三态（0=OFF, 1=AA_ONLY, 2=FULL_AUTO）=====

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>>
            SHIP_AUTO_MODE = DATA_COMPONENTS.register("ship_auto_mode",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    // ===== 武器种类标签 =====

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WeaponCategory>>
            WEAPON_CATEGORY = DATA_COMPONENTS.register("weapon_category",
            () -> DataComponentType.<WeaponCategory>builder()
                    .persistent(WeaponCategory.CODEC)
                    .networkSynchronized(WeaponCategory.STREAM_CODEC)
                    .build());

    // ===== 装备强化等级 =====

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>>
            EQUIPMENT_ENHANCEMENT_LEVEL = DATA_COMPONENTS.register("equipment_enhancement_level",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    // ===== 分槽冷却 =====

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SlotCooldowns>>
            SLOT_COOLDOWNS = DATA_COMPONENTS.register("slot_cooldowns",
            () -> DataComponentType.<SlotCooldowns>builder()
                    .persistent(SlotCooldowns.CODEC)
                    .networkSynchronized(SlotCooldowns.STREAM_CODEC)
                    .build());

    // ===== 武器冷却（直接存在武器物品上） =====

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
    /** 海上 R 键鱼雷装填的待结算状态：读条期间不扣物品，到期才扣。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.piranport.component.PendingTorpedoReload>>
            PENDING_TORPEDO_RELOAD = DATA_COMPONENTS.register("pending_torpedo_reload",
            () -> DataComponentType.<com.piranport.component.PendingTorpedoReload>builder()
                    .persistent(com.piranport.component.PendingTorpedoReload.CODEC)
                    .networkSynchronized(com.piranport.component.PendingTorpedoReload.STREAM_CODEC)
                    .build());

    // ===== 偏好弹种（Phase 4 自动装填） =====

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SelectedAmmoType>>
            SELECTED_AMMO_TYPE = DATA_COMPONENTS.register("selected_ammo_type",
            () -> DataComponentType.<SelectedAmmoType>builder()
                    .persistent(SelectedAmmoType.CODEC)
                    .networkSynchronized(SelectedAmmoType.STREAM_CODEC)
                    .build());

    // ===== 创造模式弹药库：已勾选的弹药/燃料 =====

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CreativeAmmoSelection>>
            CREATIVE_AMMO_SELECTION = DATA_COMPONENTS.register("creative_ammo_selection",
            () -> DataComponentType.<CreativeAmmoSelection>builder()
                    .persistent(CreativeAmmoSelection.CODEC)
                    .networkSynchronized(CreativeAmmoSelection.STREAM_CODEC)
                    .build());

    // ===== 舰装核心储存强化件 =====

    /** Stores enhancement items slotted into the ship core. */
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

    // ===== 自定义核心配置（改装器） =====

    /** 自定义核心配置，存储改装后的槽位数量和舰型 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomCoreConfig>>
            CUSTOM_CORE_CONFIG = DATA_COMPONENTS.register("custom_core_config",
            () -> DataComponentType.<CustomCoreConfig>builder()
                    .persistent(CustomCoreConfig.CODEC)
                    .networkSynchronized(CustomCoreConfig.STREAM_CODEC)
                    .build());

    // ===== v0.0.8 Dungeon DataComponents =====

    /** Stage ID stored on a dungeon key. */
    private static final int MAX_DUNGEON_STAGE_ID_LENGTH = 128;

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>>
            DUNGEON_STAGE_ID = DATA_COMPONENTS.register("dungeon_stage_id",
            () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.stringUtf8(MAX_DUNGEON_STAGE_ID_LENGTH))
                    .build());

    /** Instance UUID stored on a dungeon key. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.UUID>>
            DUNGEON_INSTANCE_ID = DATA_COMPONENTS.register("dungeon_instance_id",
            () -> DataComponentType.<java.util.UUID>builder()
                    .persistent(net.minecraft.core.UUIDUtil.CODEC)
                    .networkSynchronized(net.minecraft.core.UUIDUtil.STREAM_CODEC)
                    .build());

    /** Progress record stored on a dungeon key. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.piranport.dungeon.key.DungeonProgress>>
            DUNGEON_PROGRESS = DATA_COMPONENTS.register("dungeon_progress",
            () -> DataComponentType.<com.piranport.dungeon.key.DungeonProgress>builder()
                    .persistent(com.piranport.dungeon.key.DungeonProgress.CODEC)
                    .networkSynchronized(com.piranport.dungeon.key.DungeonProgress.STREAM_CODEC)
                    .build());

    /** 章节钥匙上「非当前关 → 副本实例」的记录；当前关的实例仍走 DUNGEON_INSTANCE_ID。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.Map<String, java.util.UUID>>>
            DUNGEON_STAGE_RUNS = DATA_COMPONENTS.register("dungeon_stage_runs",
            () -> DataComponentType.<java.util.Map<String, java.util.UUID>>builder()
                    .persistent(Codec.unboundedMap(Codec.STRING, net.minecraft.core.UUIDUtil.CODEC))
                    .networkSynchronized(ByteBufCodecs.map(java.util.HashMap::new,
                            ByteBufCodecs.stringUtf8(MAX_DUNGEON_STAGE_ID_LENGTH),
                            net.minecraft.core.UUIDUtil.STREAM_CODEC))
                    .build());
    // ===== 航海罗盘（海图/04）=====

    /** default / ship 两态；必须网络同步，否则客户端看不到 ship 模式的附魔光效。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>>
            NAVIGATION_MODE = DATA_COMPONENTS.register("navigation_mode",
            () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    /** 已记录的深海遗迹坐标；只给服务端选目标用，不需要同步到客户端。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.core.GlobalPos>>
            NAVIGATION_RUIN = DATA_COMPONENTS.register("navigation_ruin",
            () -> DataComponentType.<net.minecraft.core.GlobalPos>builder()
                    .persistent(net.minecraft.core.GlobalPos.CODEC)
                    .build());

}
