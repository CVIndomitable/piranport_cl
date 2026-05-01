package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.block.FlareLightBlock;
import com.piranport.block.ModelDebugBlock;
import com.piranport.block.PlaceableFoodBlock;
import com.piranport.block.ReloadFacilityBlock;
import com.piranport.block.SaltChipBlock;
import com.piranport.block.SmokeScreenBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, PiranPort.MOD_ID);

    // 1.20.1 DropExperienceBlock 的构造签名是 (Properties, IntProvider)，与 1.21.1 的 (IntProvider, Properties) 参数顺序相反。
    public static final RegistryObject<Block> BAUXITE_ORE = register("bauxite_ore",
            () -> new DropExperienceBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(3.0f, 3.0f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE),
                    UniformInt.of(1, 3)));

    public static final RegistryObject<Block> ALUMINUM_BLOCK = register("aluminum_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> SALT_BLOCK = register("salt_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(0.5f)
                    .sound(SoundType.SAND)));

    public static final RegistryObject<SaltChipBlock> SALT_CHIP = register("salt_chip",
            () -> new SaltChipBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(0.3f)
                    .sound(SoundType.SAND)
                    .noOcclusion()));

    // 烟雾弹和照明弹都不带 BlockItem —— 运行时由物品创建，生存模式无法直接获取。
    public static final RegistryObject<SmokeScreenBlock> SMOKE_SCREEN = registerNoItem("smoke_screen",
            () -> new SmokeScreenBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(-1.0f, 3600000.0f)
                    .sound(SoundType.WOOL)
                    .noOcclusion()
                    .noCollission()
                    .noLootTable()
                    .isViewBlocking((s, g, p) -> true)
                    .isSuffocating((s, g, p) -> false)
                    .isRedstoneConductor((s, g, p) -> false)));

    // 1.20.1 BlockBehaviour.Properties 没有 replaceable()（1.20.5+ 才加）；由 FlareLightBlock#canBeReplaced 替代。
    public static final RegistryObject<FlareLightBlock> FLARE_LIGHT = registerNoItem("flare_light",
            () -> new FlareLightBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .noCollission()
                    .noOcclusion()
                    .noLootTable()
                    .lightLevel(state -> 14)
                    .sound(SoundType.WOOL)
                    .instabreak()));

    public static final RegistryObject<ReloadFacilityBlock> RELOAD_FACILITY = register("reload_facility",
            () -> new ReloadFacilityBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    public static final RegistryObject<ModelDebugBlock> MODEL_DEBUG = register("model_debug",
            ModelDebugBlock::new);

    public static final RegistryObject<PlaceableFoodBlock.Plate> PLATE_FOOD = registerNoItem("plate_food",
            () -> new PlaceableFoodBlock.Plate(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .strength(0.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final RegistryObject<PlaceableFoodBlock.Bowl> BOWL_FOOD = registerNoItem("bowl_food",
            () -> new PlaceableFoodBlock.Bowl(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .strength(0.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final RegistryObject<PlaceableFoodBlock.Cake> CAKE_FOOD = registerNoItem("cake_food",
            () -> new PlaceableFoodBlock.Cake(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .strength(0.5f)
                    .sound(SoundType.WOOL)
                    .noOcclusion()));

    private static <T extends Block> RegistryObject<T> register(String name, Supplier<T> factory) {
        RegistryObject<T> block = BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static <T extends Block> RegistryObject<T> registerNoItem(String name, Supplier<T> factory) {
        return BLOCKS.register(name, factory);
    }

    private ModBlocks() {}
}
