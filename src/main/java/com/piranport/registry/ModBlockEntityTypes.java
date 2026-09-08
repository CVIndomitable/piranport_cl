package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.block.entity.AbyssalSpawnerBlockEntity;
import com.piranport.block.entity.AmmoWorkbenchBlockEntity;
import com.piranport.block.entity.BlueprintChestBlockEntity;
import com.piranport.block.entity.CookingPotBlockEntity;
import com.piranport.block.entity.CuttingBoardBlockEntity;
import com.piranport.block.entity.PlaceableFoodBlockEntity;
import com.piranport.block.entity.ReloadFacilityBlockEntity;
import com.piranport.block.entity.ShipCoreModifierBlockEntity;
import com.piranport.block.entity.SmokeScreenBlockEntity;
import com.piranport.block.entity.StoneMillBlockEntity;
import com.piranport.block.entity.StoveBlockEntity;
import com.piranport.block.entity.WeaponWorkbenchBlockEntity;
import com.piranport.block.entity.YubariWaterBucketBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PiranPort.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StoneMillBlockEntity>> STONE_MILL =
            BLOCK_ENTITY_TYPES.register("stone_mill", () ->
                    BlockEntityType.Builder.of(StoneMillBlockEntity::new, ModBlocks.STONE_MILL.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CuttingBoardBlockEntity>> CUTTING_BOARD =
            BLOCK_ENTITY_TYPES.register("cutting_board", () ->
                    BlockEntityType.Builder.of(CuttingBoardBlockEntity::new, ModBlocks.CUTTING_BOARD.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CookingPotBlockEntity>> COOKING_POT =
            BLOCK_ENTITY_TYPES.register("cooking_pot", () ->
                    BlockEntityType.Builder.of(CookingPotBlockEntity::new, ModBlocks.COOKING_POT.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StoveBlockEntity>> STOVE =
            BLOCK_ENTITY_TYPES.register("stove", () ->
                    BlockEntityType.Builder.of(StoveBlockEntity::new, ModBlocks.STOVE.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReloadFacilityBlockEntity>> RELOAD_FACILITY =
            BLOCK_ENTITY_TYPES.register("reload_facility", () ->
                    BlockEntityType.Builder.of(ReloadFacilityBlockEntity::new, ModBlocks.RELOAD_FACILITY.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.piranport.block.entity.ShipCoreModifierBlockEntity>> SHIP_CORE_MODIFIER =
            BLOCK_ENTITY_TYPES.register("ship_core_modifier", () ->
                    BlockEntityType.Builder.of(com.piranport.block.entity.ShipCoreModifierBlockEntity::new, ModBlocks.SHIP_CORE_MODIFIER.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<YubariWaterBucketBlockEntity>> YUBARI_WATER_BUCKET =
            BLOCK_ENTITY_TYPES.register("yubari_water_bucket", () ->
                    BlockEntityType.Builder.of(YubariWaterBucketBlockEntity::new, ModBlocks.YUBARI_WATER_BUCKET.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.piranport.block.entity.AmmoWorkbenchBlockEntity>> AMMO_WORKBENCH =
            BLOCK_ENTITY_TYPES.register("ammo_workbench", () ->
                    BlockEntityType.Builder.of(com.piranport.block.entity.AmmoWorkbenchBlockEntity::new,
                            ModBlocks.AMMO_WORKBENCH.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.piranport.block.entity.WeaponWorkbenchBlockEntity>> WEAPON_WORKBENCH =
            BLOCK_ENTITY_TYPES.register("weapon_workbench", () ->
                    BlockEntityType.Builder.of(com.piranport.block.entity.WeaponWorkbenchBlockEntity::new,
                            ModBlocks.WEAPON_WORKBENCH.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlueprintChestBlockEntity>> BLUEPRINT_CHEST =
            BLOCK_ENTITY_TYPES.register("blueprint_chest", () ->
                    BlockEntityType.Builder.of(BlueprintChestBlockEntity::new,
                            ModBlocks.BLUEPRINT_CHEST.get())
                            .build(null));

    // Abyssal Spawner (v0.0.11)
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.piranport.block.entity.AbyssalSpawnerBlockEntity>> ABYSSAL_SPAWNER =
            BLOCK_ENTITY_TYPES.register("abyssal_spawner", () ->
                    BlockEntityType.Builder.of(com.piranport.block.entity.AbyssalSpawnerBlockEntity::new,
                            ModBlocks.ABYSSAL_SPAWNER.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.piranport.block.entity.ModelDebugBlockEntity>> MODEL_DEBUG =
            BLOCK_ENTITY_TYPES.register("model_debug", () ->
                    BlockEntityType.Builder.of(com.piranport.block.entity.ModelDebugBlockEntity::new,
                            ModBlocks.MODEL_DEBUG.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceableFoodBlockEntity>> PLACEABLE_FOOD =
            BLOCK_ENTITY_TYPES.register("placeable_food", () ->
                    BlockEntityType.Builder.of(PlaceableFoodBlockEntity::new,
                            ModBlocks.PLATE_FOOD.get(),
                            ModBlocks.BOWL_FOOD.get(),
                            ModBlocks.CAKE_FOOD.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmokeScreenBlockEntity>> SMOKE_SCREEN =
            BLOCK_ENTITY_TYPES.register("smoke_screen", () ->
                    BlockEntityType.Builder.of(
                            SmokeScreenBlockEntity::new,
                            ModBlocks.SMOKE_SCREEN.get()
                    ).build(null));

    // 副本讲台（整合版 §2.2：钥匙插在讲台上，BE 持有）
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.piranport.dungeon.block.DungeonLecternBlockEntity>> DUNGEON_LECTERN =
            BLOCK_ENTITY_TYPES.register("dungeon_lectern", () ->
                    BlockEntityType.Builder.of(
                            com.piranport.dungeon.block.DungeonLecternBlockEntity::new,
                            ModBlocks.DUNGEON_LECTERN.get()
                    ).build(null));
}
