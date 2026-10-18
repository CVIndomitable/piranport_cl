package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.block.AbyssalSeepBlock;
import com.piranport.block.BlueprintChestBlock;
import com.piranport.block.CookingPotBlock;
import com.piranport.block.CuttingBoardBlock;
import com.piranport.block.FlareLightBlock;
import com.piranport.block.ReloadFacilityBlock;
import com.piranport.block.FourStageCropBlock;
import com.piranport.block.PlaceableFoodBlock;
import com.piranport.block.RiceCropBlock;
import com.piranport.block.SaltChipBlock;
import com.piranport.block.SmokeScreenBlock;
import com.piranport.block.StoneMillBlock;
import com.piranport.block.StoveBlock;
import com.piranport.block.ThreeStageCropBlock;
import com.piranport.block.YubariWaterBucketBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(PiranPort.MOD_ID);

    public static final DeferredBlock<Block> BAUXITE_ORE = BLOCKS.register("bauxite_ore",
            () -> new DropExperienceBlock(UniformInt.of(1, 3),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(3.0f, 3.0f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE)));

    public static final DeferredBlock<Block> ALUMINUM_BLOCK = BLOCKS.registerSimpleBlock("aluminum_block",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    public static final DeferredBlock<Block> SALT_BLOCK = BLOCKS.registerSimpleBlock("salt_block",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(0.5f)
                    .sound(SoundType.SAND));

    public static final DeferredBlock<SaltChipBlock> SALT_CHIP = BLOCKS.register("salt_chip",
            () -> new SaltChipBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(0.3f)
                    .sound(SoundType.SAND)
                    .noOcclusion()));

    // ===== Crop Blocks (Phase 11) =====
    private static BlockBehaviour.Properties cropProps() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT);
    }

    public static final DeferredBlock<FourStageCropBlock> TOMATO_CROP =
            BLOCKS.register("tomato_crop", () -> new FourStageCropBlock(cropProps(), () -> ModItems.TOMATO_SEEDS.get()));
    public static final DeferredBlock<FourStageCropBlock> SOYBEAN_CROP =
            BLOCKS.register("soybean_crop", () -> new FourStageCropBlock(cropProps(), () -> ModItems.SOYBEAN_SEEDS.get()));
    public static final DeferredBlock<FourStageCropBlock> CHILI_CROP =
            BLOCKS.register("chili_crop", () -> new FourStageCropBlock(cropProps(), () -> ModItems.CHILI_SEEDS.get()));
    public static final DeferredBlock<FourStageCropBlock> ONION_CROP =
            BLOCKS.register("onion_crop", () -> new FourStageCropBlock(cropProps(), () -> ModItems.ONION_SEEDS.get()));
    public static final DeferredBlock<RiceCropBlock> RICE_CROP =
            BLOCKS.register("rice_crop", () -> new RiceCropBlock(cropProps(), () -> ModItems.RICE_SEEDS.get()));
    public static final DeferredBlock<ThreeStageCropBlock> LETTUCE_CROP =
            BLOCKS.register("lettuce_crop", () -> new ThreeStageCropBlock(cropProps(), () -> ModItems.LETTUCE_SEEDS.get()));
    public static final DeferredBlock<ThreeStageCropBlock> GARLIC_CROP =
            BLOCKS.register("garlic_crop", () -> new ThreeStageCropBlock(cropProps(), () -> ModItems.GARLIC_SEEDS.get()));
    // Phase 27: Pineapple crop
    public static final DeferredBlock<FourStageCropBlock> PINEAPPLE_CROP =
            BLOCKS.register("pineapple_crop", () -> new FourStageCropBlock(cropProps(), () -> ModItems.PINEAPPLE_SEED.get()));

    // ===== Phase 28: Shipgirl Food Expansion Crops =====
    public static final DeferredBlock<FourStageCropBlock> LABLAB_BEAN_CROP =
            BLOCKS.register("lablab_bean_crop", () -> new FourStageCropBlock(cropProps(), () -> ModItems.LABLAB_BEAN_SEEDS.get()));
    public static final DeferredBlock<FourStageCropBlock> ORMOSIA_CROP =
            BLOCKS.register("ormosia_crop", () -> new FourStageCropBlock(cropProps(), () -> ModItems.ORMOSIA_SEEDS.get()));
    public static final DeferredBlock<FourStageCropBlock> CELERY_CROP =
            BLOCKS.register("celery_crop", () -> new FourStageCropBlock(cropProps(), () -> ModItems.CELERY_SEEDS.get()));
    public static final DeferredBlock<FourStageCropBlock> RYE_CROP =
            BLOCKS.register("rye_crop", () -> new FourStageCropBlock(cropProps(), () -> ModItems.RYE_SEEDS.get()));

    // ===== Phase 28: Peach Tree =====
    private static ResourceKey<ConfiguredFeature<?, ?>> treeFeatureKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, name));
    }

    private static DeferredBlock<RotatedPillarBlock> registerTreeLog(String name) {
        return BLOCKS.register(name, () -> new RotatedPillarBlock(
                BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));
    }

    private static DeferredBlock<LeavesBlock> registerTreeLeaves(String name) {
        return BLOCKS.register(name, () -> new LeavesBlock(
                BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)));
    }

    private static DeferredBlock<SaplingBlock> registerTreeSapling(
            String name,
            String growerName,
            ResourceKey<ConfiguredFeature<?, ?>> featureKey) {
        return BLOCKS.register(name, () -> new SaplingBlock(
                new TreeGrower("piranport:" + growerName,
                        Optional.empty(),
                        Optional.of(featureKey),
                        Optional.empty()),
                BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));
    }

    public static final ResourceKey<ConfiguredFeature<?, ?>> PEACH_TREE_FEATURE_KEY =
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "peach_tree"));

    public static final DeferredBlock<RotatedPillarBlock> PEACH_LOG =
            BLOCKS.register("peach_log", () -> new RotatedPillarBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));

    public static final DeferredBlock<LeavesBlock> PEACH_LEAVES =
            registerTreeLeaves("peach_leaves");

    public static final DeferredBlock<SaplingBlock> PEACH_SAPLING =
            BLOCKS.register("peach_sapling", () -> new SaplingBlock(
                    new TreeGrower("piranport:peach",
                            Optional.empty(),
                            Optional.of(PEACH_TREE_FEATURE_KEY),
                            Optional.empty()),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));

    public static final ResourceKey<ConfiguredFeature<?, ?>> MAIDENHAIR_TREE_FEATURE_KEY =
            treeFeatureKey("maidenhair_tree");
    public static final DeferredBlock<RotatedPillarBlock> MAIDENHAIR_LOG =
            registerTreeLog("maidenhair_log");
    public static final DeferredBlock<LeavesBlock> MAIDENHAIR_LEAVES =
            registerTreeLeaves("maidenhair_leaves");
    public static final DeferredBlock<SaplingBlock> MAIDENHAIR_SAPLING =
            registerTreeSapling("maidenhair_sapling", "maidenhair", MAIDENHAIR_TREE_FEATURE_KEY);

    public static final ResourceKey<ConfiguredFeature<?, ?>> SAGO_PALM_TREE_FEATURE_KEY =
            treeFeatureKey("sago_palm_tree");
    public static final DeferredBlock<RotatedPillarBlock> SAGO_PALM_LOG =
            registerTreeLog("sago_palm_log");
    public static final DeferredBlock<LeavesBlock> SAGO_PALM_LEAVES =
            registerTreeLeaves("sago_palm_leaves");
    public static final DeferredBlock<SaplingBlock> SAGO_PALM_SAPLING =
            registerTreeSapling("sago_palm_sapling", "sago_palm", SAGO_PALM_TREE_FEATURE_KEY);

    public static final ResourceKey<ConfiguredFeature<?, ?>> GARDENIA_TREE_FEATURE_KEY =
            treeFeatureKey("gardenia_tree");
    public static final DeferredBlock<RotatedPillarBlock> GARDENIA_LOG =
            registerTreeLog("gardenia_log");
    public static final DeferredBlock<LeavesBlock> GARDENIA_LEAVES =
            registerTreeLeaves("gardenia_leaves");
    public static final DeferredBlock<SaplingBlock> GARDENIA_SAPLING =
            registerTreeSapling("gardenia_sapling", "gardenia", GARDENIA_TREE_FEATURE_KEY);

    public static final ResourceKey<ConfiguredFeature<?, ?>> CHINESE_PLUM_TREE_FEATURE_KEY =
            treeFeatureKey("chinese_plum_tree");
    public static final DeferredBlock<RotatedPillarBlock> CHINESE_PLUM_LOG =
            registerTreeLog("chinese_plum_log");
    public static final DeferredBlock<LeavesBlock> CHINESE_PLUM_LEAVES =
            registerTreeLeaves("chinese_plum_leaves");
    public static final DeferredBlock<SaplingBlock> CHINESE_PLUM_SAPLING =
            registerTreeSapling("chinese_plum_sapling", "chinese_plum", CHINESE_PLUM_TREE_FEATURE_KEY);

    public static final ResourceKey<ConfiguredFeature<?, ?>> MAPPLE_TREE_FEATURE_KEY =
            treeFeatureKey("mapple_tree");
    public static final DeferredBlock<RotatedPillarBlock> MAPPLE_LOG =
            registerTreeLog("mapple_log");
    public static final DeferredBlock<LeavesBlock> MAPPLE_LEAVES =
            registerTreeLeaves("mapple_leaves");
    public static final DeferredBlock<SaplingBlock> MAPPLE_SAPLING =
            registerTreeSapling("mapple_sapling", "mapple", MAPPLE_TREE_FEATURE_KEY);




    // Phase 30: Apple tree (策划要求注册苹果树)
    public static final ResourceKey<ConfiguredFeature<?, ?>> APPLE_TREE_FEATURE_KEY =
            treeFeatureKey("apple_tree");
    public static final DeferredBlock<RotatedPillarBlock> APPLE_LOG =
            registerTreeLog("apple_log");
    public static final DeferredBlock<LeavesBlock> APPLE_LEAVES =
            registerTreeLeaves("apple_leaves");
    public static final DeferredBlock<SaplingBlock> APPLE_SAPLING =
            registerTreeSapling("apple_sapling", "apple", APPLE_TREE_FEATURE_KEY);

    // ===== Functional Blocks (Phase 12) =====
    public static final DeferredBlock<StoneMillBlock> STONE_MILL =
            BLOCKS.register("stone_mill", () -> new StoneMillBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(3.5f, 10.0f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE)
                            .noOcclusion()));

    // ===== Functional Blocks (Phase 13) =====
    public static final DeferredBlock<CuttingBoardBlock> CUTTING_BOARD =
            BLOCKS.register("cutting_board", () -> new CuttingBoardBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.0f, 3.0f)
                            .sound(SoundType.WOOD)
                            .noOcclusion()));

    // ===== Functional Blocks (Phase 14) =====
    public static final DeferredBlock<CookingPotBlock> COOKING_POT =
            BLOCKS.register("cooking_pot", () -> new CookingPotBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0f, 6.0f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()));

    public static final DeferredBlock<StoveBlock> STOVE =
            BLOCKS.register("stove", () -> new StoveBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(3.5f, 6.0f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE)
                            .noOcclusion()
                            .lightLevel(state -> 8)));

    // ===== Placeable Food Blocks (Phase 16) =====
    private static BlockBehaviour.Properties foodBlockProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.TERRACOTTA_WHITE)
                .strength(0.5f)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .noCollission();
    }

    public static final DeferredBlock<PlaceableFoodBlock.Plate> PLATE_FOOD =
            BLOCKS.register("plate_food", () -> new PlaceableFoodBlock.Plate(foodBlockProps()));
    public static final DeferredBlock<PlaceableFoodBlock.Bowl> BOWL_FOOD =
            BLOCKS.register("bowl_food", () -> new PlaceableFoodBlock.Bowl(foodBlockProps()));
    public static final DeferredBlock<PlaceableFoodBlock.Cake> CAKE_FOOD =
            BLOCKS.register("cake_food", () -> new PlaceableFoodBlock.Cake(foodBlockProps()));
    // 吐司面包专属食物方块（自带模型）。与物品同 id 不冲突——方块/物品分属独立注册表；
    // 放置时同名方块优先于通用容器，落位逻辑见 ModFoodItem.useOn
    public static final DeferredBlock<PlaceableFoodBlock.Toast> TOAST_BREAD =
            BLOCKS.register("toast_bread", () -> new PlaceableFoodBlock.Toast(foodBlockProps()));
    // 鱼雷果汁专属食物方块（自带玻璃杯模型），同样走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.TorpedoJuice> TORPEDO_JUICE =
            BLOCKS.register("torpedo_juice", () -> new PlaceableFoodBlock.TorpedoJuice(foodBlockProps()));
    // 鲱鱼罐头专属食物方块（竖放罐头模型），同样走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.Surstromming> SURSTROMMING =
            BLOCKS.register("surstromming", () -> new PlaceableFoodBlock.Surstromming(foodBlockProps()));
    // 海军烘豆子专属食物方块（盘装烘豆+番茄片+橄榄模型），同样走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.NavalBakedBeans> NAVAL_BAKED_BEANS =
            BLOCKS.register("naval_baked_beans", () -> new PlaceableFoodBlock.NavalBakedBeans(foodBlockProps()));
    // 马卡龙专属食物方块（盘装四色马卡龙模型），同样走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.Macaron> MACARON =
            BLOCKS.register("macaron", () -> new PlaceableFoodBlock.Macaron(foodBlockProps()));
    // 墨索里尼的屁股专属食物方块（意式军粮罐模型），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.MussolinisOo> MUSSOLINIS_OO =
            BLOCKS.register("mussolinis_oo", () -> new PlaceableFoodBlock.MussolinisOo(foodBlockProps()));
    // 萨拉米披萨专属食物方块（木托盘+整张披萨模型），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.SalamiPizza> SALAMI_PIZZA =
            BLOCKS.register("salami_pizza", () -> new PlaceableFoodBlock.SalamiPizza(foodBlockProps()));
    // 马赛鱼汤专属食物方块（浅汤盘+浓汤+汤料+盘沿面包片），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.Bouillabaisse> BOUILLABAISSE =
            BLOCKS.register("bouillabaisse", () -> new PlaceableFoodBlock.Bouillabaisse(foodBlockProps()));
    // 黑麦面包扁豆汤专属食物方块（编织篮+黑列巴+汤碗），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.PeaSoupWithRyeBread> PEA_SOUP_WITH_RYE_BREAD =
            BLOCKS.register("pea_soup_with_rye_bread", () -> new PlaceableFoodBlock.PeaSoupWithRyeBread(foodBlockProps()));
    // 东坡肉专属食物方块（白盘酱汁+花刀肉块+青菜叶），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.DongpoPork> DONGPO_PORK =
            BLOCKS.register("dongpo_pork", () -> new PlaceableFoodBlock.DongpoPork(foodBlockProps()));
    // 烧烤专属食物方块（烤炉烤网+烤肉香肠肉串），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.Barbecue> BARBECUE =
            BLOCKS.register("barbecue", () -> new PlaceableFoodBlock.Barbecue(foodBlockProps()));
    // 约克郡布丁专属食物方块（白盘黑边+四只布丁），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.YorkshirePudding> YORKSHIRE_PUDDING =
            BLOCKS.register("yorkshire_pudding", () -> new PlaceableFoodBlock.YorkshirePudding(foodBlockProps()));
    // 天妇罗荞麦面专属食物方块（竹托盘+荞麦面+炸虾海苔碗），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.TempuraSobaNoodle> TEMPURA_SOBA_NOODLE =
            BLOCKS.register("tempura_soba_noodle", () -> new PlaceableFoodBlock.TempuraSobaNoodle(foodBlockProps()));
    // 塔坦苹果挞专属食物方块（蓝边椭圆盘+两只苹果挞+樱桃），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.TarteTatin> TARTE_TATIN =
            BLOCKS.register("tarte_tatin", () -> new PlaceableFoodBlock.TarteTatin(foodBlockProps()));
    // 热狗专属食物方块（油纸+两只热狗+生菜番茄酱），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.Hotdog> HOTDOG =
            BLOCKS.register("hotdog", () -> new PlaceableFoodBlock.Hotdog(foodBlockProps()));
    // 烤鱼味增汤专属食物方块（漆碗味增汤+长盘烤鱼+紫苏+萝卜泥），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.FriedFishMisoSoup> FRIED_FISH_MISO_SOUP =
            BLOCKS.register("fried_fish_miso_soup", () -> new PlaceableFoodBlock.FriedFishMisoSoup(foodBlockProps()));
    // 班尼迪克蛋专属食物方块（松饼+荷包蛋+荷兰酱+欧芹），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.EggsBenedict> EGGS_BENEDICT =
            BLOCKS.register("eggs_benedict", () -> new PlaceableFoodBlock.EggsBenedict(foodBlockProps()));
    // 红茶司康饼专属食物方块（椭圆盘+四只司康+茶杯碟），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.BlackTeaScone> BLACK_TEA_SCONE =
            BLOCKS.register("black_tea_scone", () -> new PlaceableFoodBlock.BlackTeaScone(foodBlockProps()));
    // 汤圆专属食物方块（青花瓷碗+清汤+八颗汤圆），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.Tangyuan> TANGYUAN =
            BLOCKS.register("tangyuan", () -> new PlaceableFoodBlock.Tangyuan(foodBlockProps()));
    // 龙田烧专属食物方块（炸鸡块+青椒条+柠檬片），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.ChickenTatsuta> CHICKEN_TATSUTA =
            BLOCKS.register("chicken_tatsuta", () -> new PlaceableFoodBlock.ChickenTatsuta(foodBlockProps()));
    // 烤猪肘专属食物方块（脆皮肘子+骨头+酸菜+肉汁碟），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.Schweinshaxe> SCHWEINSHAXE =
            BLOCKS.register("schweinshaxe", () -> new PlaceableFoodBlock.Schweinshaxe(foodBlockProps()));
    // 红茶三明治专属食物方块（双层金架+手指三明治+茶杯碟），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.BlackTeaSandwich> BLACK_TEA_SANDWICH =
            BLOCKS.register("black_tea_sandwich", () -> new PlaceableFoodBlock.BlackTeaSandwich(foodBlockProps()));
    // 美式汉堡专属食物方块（面包+芝士+肉饼+番茄+生菜叠层），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.AmericanBurger> AMERICAN_BURGER =
            BLOCKS.register("american_burger", () -> new PlaceableFoodBlock.AmericanBurger(foodBlockProps()));
    // 甜豆花 / 咸豆花专属食物方块（共用白瓷碗，内容不同），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.BeetBlossom> BEET_BLOSSOM =
            BLOCKS.register("beet_blossom", () -> new PlaceableFoodBlock.BeetBlossom(foodBlockProps()));
    public static final DeferredBlock<PlaceableFoodBlock.SaltyBeanCurd> SALTY_BEAN_CURD =
            BLOCKS.register("salty_bean_curd", () -> new PlaceableFoodBlock.SaltyBeanCurd(foodBlockProps()));
    // 金镶玉包子专属食物方块（竹蒸笼+三个白包子），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.DeluxeBaozi> DELUXE_BAOZI =
            BLOCKS.register("deluxe_baozi", () -> new PlaceableFoodBlock.DeluxeBaozi(foodBlockProps()));
    // 什锦叉烧炒饭专属食物方块（蓝纹白方盘+什锦炒饭堆），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.AssortedCharSiuFriedRice> ASSORTED_CHAR_SIU_FRIED_RICE =
            BLOCKS.register("assorted_char_siu_fried_rice", () -> new PlaceableFoodBlock.AssortedCharSiuFriedRice(foodBlockProps()));
    // 辣条专属食物方块（米白方盘+三层交错辣条），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.Latiao> LATIAO =
            BLOCKS.register("latiao", () -> new PlaceableFoodBlock.Latiao(foodBlockProps()));
    // 天妇罗专属食物方块（黑盘白纸+一簇炸虾+蔬菜天妇罗+萝卜泥），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.Tempura> TEMPURA =
            BLOCKS.register("tempura", () -> new PlaceableFoodBlock.Tempura(foodBlockProps()));
    // 羊羹专属食物方块（蓝灰长方盘+四片羊羹），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.Yokan> YOKAN =
            BLOCKS.register("yokan", () -> new PlaceableFoodBlock.Yokan(foodBlockProps()));
    // 皇家海军咸牛肉专属食物方块（鼓腹橡木桶+铁箍+咸牛肉块），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.RoyalNavalSaltedBeef> ROYAL_NAVAL_SALTED_BEEF =
            BLOCKS.register("royal_naval_salted_beef", () -> new PlaceableFoodBlock.RoyalNavalSaltedBeef(foodBlockProps()));
    // 仰望星空派专属食物方块（深灰派盘+酥皮五角星+竖起的沙丁鱼），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.StargazyPie> STARGAZY_PIE =
            BLOCKS.register("stargazy_pie", () -> new PlaceableFoodBlock.StargazyPie(foodBlockProps()));
    // 麻婆豆腐专属食物方块（白瓷盘+红油豆腐块+葱花肉末），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.MapoToFu> MAPO_TOFU =
            BLOCKS.register("mapo_tofu", () -> new PlaceableFoodBlock.MapoToFu(foodBlockProps()));
    // 海军咖喱专属食物方块（椭圆盘咖喱酱+米饭+块料），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.NavalCurry> NAVAL_CURRY =
            BLOCKS.register("naval_curry", () -> new PlaceableFoodBlock.NavalCurry(foodBlockProps()));
    // 炸鱼薯条专属食物方块（报纸垫底+炸鱼柳+薯条+柠檬），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.FriedFishAndChips> FRIED_FISH_AND_CHIPS =
            BLOCKS.register("fried_fish_and_chips", () -> new PlaceableFoodBlock.FriedFishAndChips(foodBlockProps()));
    // 苹果派专属食物方块（深灰金属派盘+格纹酥皮），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.ApplePie> APPLE_PIE =
            BLOCKS.register("apple_pie", () -> new PlaceableFoodBlock.ApplePie(foodBlockProps()));
    // 图林根肠啤酒专属食物方块（椭圆盘烤肠拼盘+一杯啤酒），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.ThuringerRostbratwurstUndBier> THURINGER_ROSTBRATWURST_UND_BIER =
            BLOCKS.register("thuringer_rostbratwurst_und_bier", () -> new PlaceableFoodBlock.ThuringerRostbratwurstUndBier(foodBlockProps()));
    // 白香肠面包圈专属食物方块（蓝白菱纹椭圆盘+面包圈+白香肠+肉丸），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.WeisswurstMitDerBagel> WEISSWURST_MIT_DER_BAGEL =
            BLOCKS.register("weisswurst_mit_der_bagel", () -> new PlaceableFoodBlock.WeisswurstMitDerBagel(foodBlockProps()));
    // 黑森林蛋糕专属食物方块（白瓷方盘+巧克力蛋糕+奶油樱桃），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.BlackForestGateau> BLACK_FOREST_GATEAU =
            BLOCKS.register("black_forest_gateau", () -> new PlaceableFoodBlock.BlackForestGateau(foodBlockProps()));
    // 罗宋汤专属食物方块（白瓷底碟+宽口汤盘+酸奶油+欧芹叶），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.Borscht> BORSCHT =
            BLOCKS.register("borscht", () -> new PlaceableFoodBlock.Borscht(foodBlockProps()));
    // 俄式饺子专属食物方块（碗装奶油小饺子+香草），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.RussianDumpling> RUSSIAN_DUMPLING =
            BLOCKS.register("russian_dumpling", () -> new PlaceableFoodBlock.RussianDumpling(foodBlockProps()));
    // 俄式肉冻专属食物方块（平盘上琥珀冻块金字塔堆），走"同名方块优先"落位
    public static final DeferredBlock<PlaceableFoodBlock.Okroshka> OKROSHKA =
            BLOCKS.register("okroshka", () -> new PlaceableFoodBlock.Okroshka(foodBlockProps()));

    // ===== Reload Facility =====
    public static final DeferredBlock<ReloadFacilityBlock> RELOAD_FACILITY =
            BLOCKS.register("reload_facility", () -> new ReloadFacilityBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5f, 6.0f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()));

    // ===== Ship Core Modifier =====
    public static final DeferredBlock<com.piranport.block.ShipCoreModifierBlock> SHIP_CORE_MODIFIER =
            BLOCKS.register("ship_core_modifier", () -> new com.piranport.block.ShipCoreModifierBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5f, 6.0f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)));

    // ===== Yubari Water Bucket =====
    public static final DeferredBlock<YubariWaterBucketBlock> YUBARI_WATER_BUCKET =
            BLOCKS.register("yubari_water_bucket", () -> new YubariWaterBucketBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.0f, 3.0f)
                            .sound(SoundType.WOOD)
                            .noOcclusion()));

    // ===== Ammo Workbench =====
    public static final DeferredBlock<com.piranport.block.AmmoWorkbenchBlock> AMMO_WORKBENCH =
            BLOCKS.register("ammo_workbench", () -> new com.piranport.block.AmmoWorkbenchBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5f, 6.0f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()));

    // ===== Weapon Workbench =====
    public static final DeferredBlock<com.piranport.block.WeaponWorkbenchBlock> WEAPON_WORKBENCH =
            BLOCKS.register("weapon_workbench", () -> new com.piranport.block.WeaponWorkbenchBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5f, 6.0f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()));

    public static final DeferredBlock<BlueprintChestBlock> BLUEPRINT_CHEST =
            BLOCKS.register("blueprint_chest", () -> new BlueprintChestBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.5f, 4.0f)
                            .sound(SoundType.WOOD)
                            .noOcclusion()));

    // ===== Smoke Screen =====
    public static final DeferredBlock<SmokeScreenBlock> SMOKE_SCREEN =
            BLOCKS.register("smoke_screen", () -> new SmokeScreenBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_GRAY)
                            .strength(-1.0f, 3600000.0f)
                            .sound(SoundType.WOOL)
                            .noOcclusion()
                            .noCollission()
                            .noLootTable()
                            .isViewBlocking((s, g, p) -> true)
                            .isSuffocating((s, g, p) -> false)
                            .isRedstoneConductor((s, g, p) -> false)));

    // ===== Flare Light =====
    public static final DeferredBlock<FlareLightBlock> FLARE_LIGHT =
            BLOCKS.register("flare_light", () -> new FlareLightBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.NONE)
                            .noOcclusion()
                            .noLootTable()
                            .replaceable()
                            .lightLevel(state -> 14)
                            .sound(SoundType.WOOL)
                            .instabreak()));

    // ===== Abyssal Spawner (v0.0.11) =====
    public static final DeferredBlock<com.piranport.block.AbyssalSpawnerBlock> ABYSSAL_SPAWNER =
            BLOCKS.register("abyssal_spawner",
                    () -> new com.piranport.block.AbyssalSpawnerBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.DEEPSLATE)
                                    .strength(-1.0f, 3600000.0f)
                                    .noLootTable()
                                    .noOcclusion()));

    public static final DeferredBlock<AbyssalSeepBlock> ABYSSAL_SEEP =
            BLOCKS.register("abyssal_seep",
                    () -> new AbyssalSeepBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_PURPLE)
                                    .strength(4.0f, 9.0f)
                                    .requiresCorrectToolForDrops()
                                    .sound(SoundType.DEEPSLATE)
                                    .lightLevel(state -> 6)));

    // ===== Decorative Blocks (from sheropshire) =====
    public static final DeferredBlock<com.piranport.block.ConfidentialCargoBlock> CONFIDENTIAL_CARGO =
            BLOCKS.register("confidential_cargo",
                    () -> new com.piranport.block.ConfidentialCargoBlock());

    public static final DeferredBlock<com.piranport.block.AbyssRedSpiderLilyBlock> ABYSS_RED_SPIDER_LILY =
            BLOCKS.register("abyss_red_spider_lily",
                    () -> new com.piranport.block.AbyssRedSpiderLilyBlock());

    public static final DeferredBlock<com.piranport.block.ItalianDishKitBlock> ITALIAN_DISH_KIT =
            BLOCKS.register("italian_dish_kit",
                    () -> new com.piranport.block.ItalianDishKitBlock());

    public static final DeferredBlock<com.piranport.block.PirateChairBlock> PIRATE_CHAIR =
            BLOCKS.register("pirate_chair",
                    () -> new com.piranport.block.PirateChairBlock());

    public static final DeferredBlock<com.piranport.block.PirateTableBlock> PIRATE_TABLE =
            BLOCKS.register("pirate_table",
                    () -> new com.piranport.block.PirateTableBlock());

    public static final DeferredBlock<com.piranport.block.B25ModelBlock> B25_MODEL =
            BLOCKS.register("b25_model",
                    () -> new com.piranport.block.B25ModelBlock());

    public static final DeferredBlock<com.piranport.block.ModelDebugBlock> MODEL_DEBUG =
            BLOCKS.register("model_debug",
                    () -> new com.piranport.block.ModelDebugBlock());

    // ===== Dungeon System (v0.0.8) =====
    public static final DeferredBlock<com.piranport.dungeon.block.DungeonLecternBlock> DUNGEON_LECTERN =
            BLOCKS.register("dungeon_lectern",
                    () -> new com.piranport.dungeon.block.DungeonLecternBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.DEEPSLATE)
                                    .strength(5.0f, 1200.0f)
                                    .sound(SoundType.DEEPSLATE)
                                    .noOcclusion()));

    // ===== Dungeon Portal System =====
    public static final DeferredBlock<com.piranport.dungeon.block.DungeonPortalBlock> DUNGEON_PORTAL =
            BLOCKS.register("dungeon_portal",
                    () -> new com.piranport.dungeon.block.DungeonPortalBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.DEEPSLATE)
                                    .strength(5.0f, 1200.0f)
                                    .sound(SoundType.DEEPSLATE)
                                    .noOcclusion()));

    /** 副本记录点隐形触发方块；仅由副本生成器放置，不提供生存物品。 */
    public static final DeferredBlock<com.piranport.dungeon.block.DungeonCheckpointBlock> DUNGEON_CHECKPOINT =
            BLOCKS.register("dungeon_checkpoint",
                    () -> new com.piranport.dungeon.block.DungeonCheckpointBlock(
                            BlockBehaviour.Properties.of().noOcclusion().noCollission().strength(-1.0f, 3600000.0f)));
}
