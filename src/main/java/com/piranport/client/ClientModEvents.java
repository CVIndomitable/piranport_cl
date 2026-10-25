package com.piranport.client;

import com.piranport.PiranPort;
import com.piranport.client.AircraftRenderer;
import com.piranport.client.CuttingBoardRenderer;
import com.piranport.client.PlaceableFoodRenderer;
import com.piranport.client.WeaponReloadDecorator;
import com.piranport.menu.AmmoWorkbenchScreen;
import com.piranport.menu.CookingPotScreen;
import com.piranport.menu.ReloadFacilityScreen;
import com.piranport.menu.StoneMillScreen;
import com.piranport.registry.ModBlockEntityTypes;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModMenuTypes;
import com.piranport.registry.WeaponItems;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.lwjgl.glfw.GLFW;
import net.neoforged.fml.common.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT, bus = Bus.MOD)
public class ClientModEvents {

    /** 2026-10-18 批量主炮：共用 CatalogGunRenderer，改型与原型可共享同一套素材。 */
    private static final CatalogGunRenderer.Spec JP_356 = CatalogGunRenderer.spec(
            "japanese_twin_356mm_gun", ModItems.JAPANESE_TWIN_356MM_GUN);
    private static final CatalogGunRenderer.Spec US_14 = CatalogGunRenderer.spec(
            "us_triple_14inch_gun", ModItems.US_TRIPLE_14INCH_GUN, ModItems.US_TRIPLE_14INCH_GUN_KAI);
    private static final CatalogGunRenderer.Spec BRIT_QUAD_14 = CatalogGunRenderer.spec(
            "british_quad_14inch_gun", ModItems.BRITISH_QUAD_14INCH_GUN);
    private static final CatalogGunRenderer.Spec BRIT_QUAD_14_284 = CatalogGunRenderer.spec(
            "british_quad_14inch_gun_284", ModItems.BRITISH_QUAD_14INCH_GUN_284);
    private static final CatalogGunRenderer.Spec BRIT_TWIN_15 = CatalogGunRenderer.spec(
            "british_twin_15inch_gun", ModItems.BRITISH_TWIN_15INCH_GUN);
    private static final CatalogGunRenderer.Spec BRIT_TWIN_15_KAI = CatalogGunRenderer.spec(
            "british_twin_15inch_gun_kai", ModItems.BRITISH_TWIN_15INCH_GUN_KAI);
    private static final CatalogGunRenderer.Spec IT_381 = CatalogGunRenderer.spec(
            "italian_triple_381mm_gun", ModItems.ITALIAN_TRIPLE_381MM_GUN);
    private static final CatalogGunRenderer.Spec IT_381_KAI = CatalogGunRenderer.spec(
            "italian_triple_381mm_gun_kai", ModItems.ITALIAN_TRIPLE_381MM_GUN_KAI);
    private static final CatalogGunRenderer.Spec US_16 = CatalogGunRenderer.spec(
            "us_twin_16inch_gun", ModItems.US_TWIN_16INCH_GUN, ModItems.US_TWIN_16INCH_GUN_KAI);
    private static final CatalogGunRenderer.Spec JP_410 = CatalogGunRenderer.spec(
            "japanese_twin_410mm_gun", ModItems.JAPANESE_TWIN_410MM_GUN);
    private static final CatalogGunRenderer.Spec DE_406 = CatalogGunRenderer.spec(
            "german_twin_406mm_gun", ModItems.GERMAN_TWIN_406MM_GUN);
    private static final CatalogGunRenderer.Spec BRIT_16 = CatalogGunRenderer.spec(
            "british_triple_16inch_gun", ModItems.BRITISH_TRIPLE_16INCH_GUN);
    private static final CatalogGunRenderer.Spec BRIT_16_KAI = CatalogGunRenderer.spec(
            "british_triple_16inch_gun_kai", ModItems.BRITISH_TRIPLE_16INCH_GUN_KAI);
    private static final CatalogGunRenderer.Spec US_16_MK6 = CatalogGunRenderer.spec(
            "us_triple_16inch_gun_mk6", ModItems.US_TRIPLE_16INCH_GUN_MK6);
    private static final CatalogGunRenderer.Spec US_16_MK7 = CatalogGunRenderer.spec(
            "us_triple_16inch_gun_mk7", ModItems.US_TRIPLE_16INCH_GUN_MK7);
    private static final CatalogGunRenderer.Spec DE_SKC40_420 = CatalogGunRenderer.spec(
            "german_skc40_triple_420mm_gun", ModItems.GERMAN_SKC40_TRIPLE_420MM_GUN);
    private static final CatalogGunRenderer.Spec BRIT_SINGLE_18 = CatalogGunRenderer.spec(
            "british_single_18inch_gun", ModItems.BRITISH_SINGLE_18INCH_GUN);
    private static final CatalogGunRenderer.Spec BRIT_TWIN_18 = CatalogGunRenderer.spec(
            "british_twin_18inch_gun", ModItems.BRITISH_TWIN_18INCH_GUN);
    private static final CatalogGunRenderer.Spec US_TWIN_18 = CatalogGunRenderer.spec(
            "us_twin_18inch_gun", ModItems.US_TWIN_18INCH_GUN);
    private static final CatalogGunRenderer.Spec JP_460 = CatalogGunRenderer.spec(
            "japanese_triple_460mm_gun", ModItems.JAPANESE_TRIPLE_460MM_GUN);
    private static final CatalogGunRenderer.Spec SOV_460 = CatalogGunRenderer.spec(
            "soviet_triple_460mm_gun", ModItems.SOVIET_TRIPLE_460MM_GUN);
    private static final CatalogGunRenderer.Spec BRIT_20_TWIN = CatalogGunRenderer.spec(
            "british_twin_20inch_gun", ModItems.BRITISH_TWIN_20INCH_GUN);
    private static final CatalogGunRenderer.Spec US_20_TWIN = CatalogGunRenderer.spec(
            "us_twin_20inch_gun", ModItems.US_TWIN_20INCH_GUN);

    /** 2026-10-25 批次：中口径主炮的手持 3D 模型，统一走 CatalogGunRenderer。 */
    private static final CatalogGunRenderer.Spec DE_SINGLE_127 =
            CatalogGunRenderer.spec("german_single_127mm_gun",
                    WeaponItems.CATALOG_GUNS.get("german_single_127mm_gun"));
    private static final CatalogGunRenderer.Spec US_SINGLE_5IN =
            CatalogGunRenderer.spec("us_single_5inch_gun",
                    WeaponItems.CATALOG_GUNS.get("us_single_5inch_gun"));
    private static final CatalogGunRenderer.Spec US_TWIN_5IN =
            CatalogGunRenderer.spec("us_twin_5inch_gun",
                    WeaponItems.CATALOG_GUNS.get("us_twin_5inch_gun"));
    private static final CatalogGunRenderer.Spec US_TWIN_5IN_DP =
            CatalogGunRenderer.spec("us_twin_5inch_dp_gun",
                    WeaponItems.CATALOG_GUNS.get("us_twin_5inch_dp_gun"));
    private static final CatalogGunRenderer.Spec TWIN_5IN_L54_DP =
            CatalogGunRenderer.spec("twin_5inch_l54_dp_gun",
                    WeaponItems.CATALOG_GUNS.get("twin_5inch_l54_dp_gun"));
    private static final CatalogGunRenderer.Spec JP_SINGLE_140 =
            CatalogGunRenderer.spec("japanese_single_140mm_gun",
                    WeaponItems.CATALOG_GUNS.get("japanese_single_140mm_gun"));
    private static final CatalogGunRenderer.Spec BRIT_SINGLE_6 =
            CatalogGunRenderer.spec("british_single_6inch_gun",
                    WeaponItems.CATALOG_GUNS.get("british_single_6inch_gun"));
    private static final CatalogGunRenderer.Spec BRIT_TWIN_6 =
            CatalogGunRenderer.spec("british_twin_6inch_gun",
                    WeaponItems.CATALOG_GUNS.get("british_twin_6inch_gun"));
    private static final CatalogGunRenderer.Spec JP_TWIN_152 =
            CatalogGunRenderer.spec("japanese_twin_152mm_gun",
                    WeaponItems.CATALOG_GUNS.get("japanese_twin_152mm_gun"));
    private static final CatalogGunRenderer.Spec BRIT_TRIPLE_6 =
            CatalogGunRenderer.spec("british_triple_6inch_gun",
                    WeaponItems.CATALOG_GUNS.get("british_triple_6inch_gun"));
    private static final CatalogGunRenderer.Spec US_TRIPLE_6 =
            CatalogGunRenderer.spec("us_triple_6inch_gun",
                    WeaponItems.CATALOG_GUNS.get("us_triple_6inch_gun"));
    private static final CatalogGunRenderer.Spec MK16_TRIPLE_6 =
            CatalogGunRenderer.spec("mk16_triple_6inch_gun_flashless",
                    WeaponItems.CATALOG_GUNS.get("mk16_triple_6inch_gun_flashless"));
    /** 2026-10-25 批次二：三联 6 英寸 / 三联 180 毫米 / 双联 8 英寸。 */
    private static final CatalogGunRenderer.Spec IMPROVED_TRIPLE_6 =
            CatalogGunRenderer.spec("improved_triple_6inch_gun",
                    WeaponItems.CATALOG_GUNS.get("improved_triple_6inch_gun"));
    private static final CatalogGunRenderer.Spec TRIPLE_152_DP_46 =
            CatalogGunRenderer.spec("triple_152mm_dp_gun_46",
                    WeaponItems.CATALOG_GUNS.get("triple_152mm_dp_gun_46"));
    private static final CatalogGunRenderer.Spec SWEDISH_TRIPLE_152 =
            CatalogGunRenderer.spec("swedish_triple_152mm_gun",
                    WeaponItems.CATALOG_GUNS.get("swedish_triple_152mm_gun"));
    private static final CatalogGunRenderer.Spec SOVIET_TRIPLE_180 =
            CatalogGunRenderer.spec("soviet_triple_180mm_gun",
                    WeaponItems.CATALOG_GUNS.get("soviet_triple_180mm_gun"));
    private static final CatalogGunRenderer.Spec SOVIET_IMPROVED_TRIPLE_180 =
            CatalogGunRenderer.spec("soviet_improved_triple_180mm_gun",
                    WeaponItems.CATALOG_GUNS.get("soviet_improved_triple_180mm_gun"));
    private static final CatalogGunRenderer.Spec BRIT_TWIN_8 =
            CatalogGunRenderer.spec("british_twin_8inch_gun",
                    WeaponItems.CATALOG_GUNS.get("british_twin_8inch_gun"));
    private static final CatalogGunRenderer.Spec JP_TWIN_203 =
            CatalogGunRenderer.spec("japanese_twin_203mm_gun",
                    WeaponItems.CATALOG_GUNS.get("japanese_twin_203mm_gun"));

    /** 2026-10-25 批次：图鉴鱼雷发射器的手持 3D 模型，统一走 CatalogTorpedoLauncherRenderer。 */
    private static final CatalogTorpedoLauncherRenderer.Spec QUINT_533_MK17 =
            CatalogTorpedoLauncherRenderer.spec("quintuple_533mm_torpedo_mk17",
                    ModItems.QUINTUPLE_533MM_TORPEDO_MK17);
    private static final CatalogTorpedoLauncherRenderer.Spec QUINT_21IN_MK35 =
            CatalogTorpedoLauncherRenderer.spec("quintuple_21in_torpedo_mk35",
                    ModItems.QUINTUPLE_21IN_TORPEDO_MK35);
    private static final CatalogTorpedoLauncherRenderer.Spec QUINT_53_56 =
            CatalogTorpedoLauncherRenderer.spec("quintuple_torpedo_launcher_53_56",
                    ModItems.QUINTUPLE_TORPEDO_LAUNCHER_53_56);
    private static final CatalogTorpedoLauncherRenderer.Spec QUINT_53_65 =
            CatalogTorpedoLauncherRenderer.spec("quintuple_torpedo_launcher_53_65",
                    ModItems.QUINTUPLE_TORPEDO_LAUNCHER_53_65);
    private static final CatalogTorpedoLauncherRenderer.Spec TRIPLE_61CM =
            CatalogTorpedoLauncherRenderer.spec("triple_61cm_torpedo",
                    ModItems.TRIPLE_61CM_TORPEDO);
    private static final CatalogTorpedoLauncherRenderer.Spec TRIPLE_61CM_OXY =
            CatalogTorpedoLauncherRenderer.spec("triple_61cm_oxygen_torpedo",
                    ModItems.TRIPLE_61CM_OXYGEN_TORPEDO);
    /** 四连 61 厘米常规型与氧气型共用同一张原画。 */
    private static final CatalogTorpedoLauncherRenderer.Spec QUAD_61CM =
            CatalogTorpedoLauncherRenderer.spec("quad_61cm_torpedo",
                    ModItems.QUAD_61CM_TORPEDO, ModItems.QUAD_61CM_OXYGEN_TORPEDO);
    private static final CatalogTorpedoLauncherRenderer.Spec LAUNCHER_61CM_TYPE9 =
            CatalogTorpedoLauncherRenderer.spec("torpedo_launcher_61cm_type9",
                    ModItems.TORPEDO_LAUNCHER_61CM_TYPE9);
    private static final CatalogTorpedoLauncherRenderer.Spec QUINT_61CM =
            CatalogTorpedoLauncherRenderer.spec("quintuple_61cm_torpedo",
                    ModItems.QUINTUPLE_61CM_TORPEDO);
    private static final CatalogTorpedoLauncherRenderer.Spec QUINT_61CM_OXY =
            CatalogTorpedoLauncherRenderer.spec("quintuple_61cm_oxygen_torpedo",
                    ModItems.QUINTUPLE_61CM_OXYGEN_TORPEDO);
    private static final CatalogTorpedoLauncherRenderer.Spec JP_SEXTUPLE_61CM =
            CatalogTorpedoLauncherRenderer.spec("japanese_sextuple_61cm_torpedo",
                    ModItems.JAPANESE_SEXTUPLE_61CM_TORPEDO);
    private static final CatalogTorpedoLauncherRenderer.Spec SEXTUPLE_93M3 =
            CatalogTorpedoLauncherRenderer.spec("sextuple_61cm_torpedo_launcher_93m3",
                    ModItems.SEXTUPLE_61CM_TORPEDO_LAUNCHER_93M3);
    private static final CatalogTorpedoLauncherRenderer.Spec SEPTUPLE =
            CatalogTorpedoLauncherRenderer.spec("septuple_torpedo_launcher",
                    ModItems.SEPTUPLE_TORPEDO_LAUNCHER);
    private static final CatalogTorpedoLauncherRenderer.Spec SEPTUPLE_93M3 =
            CatalogTorpedoLauncherRenderer.spec("septuple_torpedo_launcher_93m3",
                    ModItems.SEPTUPLE_TORPEDO_LAUNCHER_93M3);
    private static final CatalogTorpedoLauncherRenderer.Spec JP_TRIPLE_72CM =
            CatalogTorpedoLauncherRenderer.spec("japanese_triple_72cm_torpedo",
                    ModItems.JAPANESE_TRIPLE_72CM_TORPEDO);
    private static final CatalogTorpedoLauncherRenderer.Spec CLT_QUAD_TYPE0 =
            CatalogTorpedoLauncherRenderer.spec("clt_quad_torpedo_launcher_type0",
                    ModItems.CLT_QUAD_TORPEDO_LAUNCHER_TYPE0);

    /** 注册日本12厘米单装炮、日本12.7厘米连装炮、博福斯双联120毫米炮与德国/意大利/美国三联主炮的 standalone 3D 模型（BEWLR 直接取用）。 */
    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(Japanese12cmSingleGunRenderer.MODEL_EMPTY);
        event.register(Japanese12cmSingleGunRenderer.MODEL_LOADED);
        event.register(Japanese127mmTwinGunRenderer.MODEL_EMPTY);
        event.register(Japanese127mmTwinGunRenderer.MODEL_LOADED);
        event.register(GermanTwin203mmGunRenderer.MODEL_EMPTY);
        event.register(GermanTwin203mmGunRenderer.MODEL_LOADED);
        event.register(GermanTriple203mmGunRenderer.MODEL_EMPTY);
        event.register(GermanTriple203mmGunRenderer.MODEL_LOADED);
        event.register(GermanTriple283mmGunRenderer.MODEL_EMPTY);
        event.register(GermanTriple283mmGunRenderer.MODEL_LOADED);
        event.register(ItalianTriple305mmGunRenderer.MODEL_EMPTY);
        event.register(ItalianTriple305mmGunRenderer.MODEL_LOADED);
        event.register(UsTriple12inchGunRenderer.MODEL_EMPTY);
        event.register(UsTriple12inchGunRenderer.MODEL_LOADED);
        event.register(ItalianTriple320mmGunRenderer.MODEL_EMPTY);
        event.register(ItalianTriple320mmGunRenderer.MODEL_LOADED);
        event.register(BoforsTwin120mmGunRenderer.MODEL_EMPTY);
        event.register(BoforsTwin120mmGunRenderer.MODEL_LOADED);
        event.register(Quad533mmTorpedoRenderer.MODEL_EMPTY);
        event.register(Quad533mmTorpedoRenderer.MODEL_LOADED);
        event.register(Quad533mmTorpedoRenderer.MODEL_EMPTY_UR);
        event.register(Quad533mmTorpedoRenderer.MODEL_LOADED_UR);
        event.register(Quintuple533mmTorpedoRenderer.MODEL_EMPTY);
        event.register(Quintuple533mmTorpedoRenderer.MODEL_LOADED);
        for (CatalogTorpedoLauncherRenderer.Spec spec : CatalogTorpedoLauncherRenderer.specs()) {
            event.register(spec.modelEmpty());
            event.register(spec.modelLoaded());
        }
        for (CatalogGunRenderer.Spec spec : CatalogGunRenderer.specs()) {
            event.register(spec.modelEmpty());
            event.register(spec.modelLoaded());
        }
    }

    /** 给日本12厘米单装炮、日本12.7厘米连装炮、博福斯双联120毫米炮与德国/意大利/美国三联主炮挂自定义渲染器（BEWLR）。 */
    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(Japanese12cmSingleGunRenderer.CLIENT_EXTENSIONS,
                ModItems.SINGLE_SMALL_GUN.get());
        event.registerItem(Japanese127mmTwinGunRenderer.CLIENT_EXTENSIONS,
                ModItems.JAPANESE_127MM_TWIN_GUN.get());
        event.registerItem(GermanTwin203mmGunRenderer.CLIENT_EXTENSIONS,
                ModItems.GERMAN_TWIN_203MM_GUN.get());
        event.registerItem(GermanTriple203mmGunRenderer.CLIENT_EXTENSIONS,
                ModItems.GERMAN_TRIPLE_203MM_GUN.get());
        event.registerItem(GermanTriple283mmGunRenderer.CLIENT_EXTENSIONS,
                ModItems.GERMAN_TRIPLE_283MM_GUN.get());
        event.registerItem(ItalianTriple305mmGunRenderer.CLIENT_EXTENSIONS,
                ModItems.ITALIAN_TRIPLE_305MM_GUN.get());
        event.registerItem(UsTriple12inchGunRenderer.CLIENT_EXTENSIONS,
                ModItems.US_TRIPLE_12INCH_GUN.get());
        event.registerItem(ItalianTriple320mmGunRenderer.CLIENT_EXTENSIONS,
                ModItems.ITALIAN_TRIPLE_320MM_GUN.get());
        event.registerItem(BoforsTwin120mmGunRenderer.CLIENT_EXTENSIONS,
                ModItems.BOFORS_TWIN_120MM_GUN.get());
        // 四联533毫米鱼雷（本体与 ur 高速型共用一套发射器渲染）
        event.registerItem(Quad533mmTorpedoRenderer.CLIENT_EXTENSIONS,
                ModItems.QUAD_533MM_TORPEDO.get(),
                ModItems.QUAD_533MM_TORPEDO_UR_HIGH_SPEED.get());
        // 五联533毫米鱼雷
        event.registerItem(Quintuple533mmTorpedoRenderer.CLIENT_EXTENSIONS,
                ModItems.QUINTUPLE_533MM_TORPEDO.get());
        for (CatalogTorpedoLauncherRenderer.Spec spec : CatalogTorpedoLauncherRenderer.specs()) {
            for (var item : spec.items()) {
                event.registerItem(CatalogTorpedoLauncherRenderer.extensionsFor(spec), item.get());
            }
        }
        for (CatalogGunRenderer.Spec spec : CatalogGunRenderer.specs()) {
            for (var item : spec.items()) {
                event.registerItem(CatalogGunRenderer.extensionsFor(spec), item.get());
            }
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 初始化皮肤核心 3D 渲染器（需要在客户端主线程访问 EntityModelSet）
        event.enqueueWork(SkinCoreItemRenderer::init);
        // 日本12厘米单装炮：物品栏画 2D 图标，手持渲染 3D 炮塔
        event.enqueueWork(Japanese12cmSingleGunRenderer::init);
        // 日本12.7厘米连装炮：物品栏画 2D 图标，手持渲染 3D 炮塔
        event.enqueueWork(Japanese127mmTwinGunRenderer::init);
        // 德国双联203毫米炮：同一套 BEWLR 方案
        event.enqueueWork(GermanTwin203mmGunRenderer::init);
        // 德国三联203毫米炮：同一套 BEWLR 方案
        event.enqueueWork(GermanTriple203mmGunRenderer::init);
        // 德国三联283毫米炮：同一套 BEWLR 方案
        event.enqueueWork(GermanTriple283mmGunRenderer::init);
        // 意大利三联305毫米炮：同一套 BEWLR 方案
        event.enqueueWork(ItalianTriple305mmGunRenderer::init);
        // 美国三联12英寸炮：同一套 BEWLR 方案
        event.enqueueWork(UsTriple12inchGunRenderer::init);
        // 意大利三联320毫米炮：同一套 BEWLR 方案
        event.enqueueWork(ItalianTriple320mmGunRenderer::init);
        // 博福斯双联120毫米炮：同一套 BEWLR 方案
        event.enqueueWork(BoforsTwin120mmGunRenderer::init);
        // 四联533毫米鱼雷：鱼雷发射器走同一套 BEWLR 方案
        event.enqueueWork(Quad533mmTorpedoRenderer::init);
        // 五联533毫米鱼雷：同一套 BEWLR 方案
        event.enqueueWork(Quintuple533mmTorpedoRenderer::init);
        // 图鉴鱼雷发射器批次：共用 CatalogTorpedoLauncherRenderer
        event.enqueueWork(CatalogTorpedoLauncherRenderer::initAll);
        // 2026-10-18 批量主炮：共用 CatalogGunRenderer
        event.enqueueWork(CatalogGunRenderer::initAll);

        // 注册 "fueled" 物品属性：有燃料时为 1.0，否则为 0.0。
        // 模型使用此谓词在装载和空载纹理之间切换。
        ResourceLocation fueled = ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "fueled");
        // 注册 "loaded" 物品属性：已装填弹药时为 1.0，否则为 0.0。
        // 火炮/鱼雷发射器/导弹发射器用它切换"空膛/已装填"纹理。
        // WHY：火炮走"类似弩"的装填模型（见 docs/策划决策/武器/09-装填类似弩模型.md），
        // 空膛时无法发射，玩家需要一眼看出武器是否能开火，因此必须有独立的空膛贴图。
        ResourceLocation loaded = ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "loaded");
        ResourceLocation blocking = ResourceLocation.withDefaultNamespace("blocking");
        // 注意：这里**没有** "ammo_type" 属性，而且不要加回来。
        // WHY：原版 1.21.1 的 overrides[].predicate 只有 (ResourceLocation, float) 一种类型，
        // 没有字符串/枚举谓词，判定又是 "value >= threshold" 且 ItemOverrides 倒序遍历数组，
        // 所以多谓词覆盖**无法表达弹种的互斥取值**（弹药种类 N 也会同时满足种类 M 的阈值）。
        // 目前物品图标只通过 loaded 谓词切换空膛/装填两态；弹种差异不在模型层用浮点魔法数字区分。
        // 航海罗盘复用原版指南针的 angle 属性与指针帧。1.21.1 的物品属性按物品注册，
        // 不给自定义物品注册就永远不会转，模型 overrides 会一直停在 0。
        CompassItemPropertyFunction.CompassTarget compassTarget = (level, stack, entity) -> {
            LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
            return tracker != null ? tracker.target().orElse(null) : null;
        };
        event.enqueueWork(() -> ItemProperties.register(ModItems.NAVIGATION_COMPASS.get(),
                ResourceLocation.withDefaultNamespace("angle"),
                new CompassItemPropertyFunction(compassTarget)));

        event.enqueueWork(() -> {
            // 为所有 AircraftItem 实例注册 "fueled" 属性（包括命名变体）
            for (var entry : ModItems.ITEMS.getEntries()) {
                if (entry.get() instanceof com.piranport.item.TaihouUmbrellaItem
                        || entry.get() instanceof com.piranport.item.EugenShieldItem) {
                    ItemProperties.register(entry.get(), blocking, (stack, level, entity, seed) ->
                            entity != null && entity.isUsingItem()
                                    && entity.getUseItem().getItem() == stack.getItem()
                                    ? 1.0f : 0.0f);
                }
                if (entry.get() instanceof com.piranport.item.AircraftItem) {
                    ItemProperties.register(entry.get(), fueled, (stack, level, entity, seed) ->
                            com.piranport.aviation.AircraftDefinitionService.stateOf(stack).currentFuel() > 0
                                    ? 1.0f : 0.0f);
                }
                // 为所有火炮和发射器（鱼雷/导弹/火箭）注册 "loaded" 属性。
                // 判定依据是 LOADED_AMMO 数据组件，而非某种物品子类特有的状态，
                // 这样鱼雷再装填增强件（走同一组件）生效时也会自动切换贴图。
                // 注意：导弹发射器是 MissileLauncherItem，与 TorpedoLauncherItem
                // 没有继承关系，两者都要覆盖，否则导弹发射器贴图不会切换。
                if (entry.get() instanceof com.piranport.artillery.ArtilleryItem
                        || entry.get() instanceof com.piranport.item.TorpedoLauncherItem
                        || entry.get() instanceof com.piranport.item.MissileLauncherItem) {
                    ItemProperties.register(entry.get(), loaded, (stack, level, entity, seed) -> {
                        var ammo = stack.get(ModDataComponents.LOADED_AMMO.get());
                        return (ammo != null && ammo.hasAmmo()) ? 1.0f : 0.0f;
                    });
                }
            }
        });
    }

    /** 副本书台纹路面（tintindex 0）按 PATTERN 染白/红/绿，不另画贴图（副本/17 §3.2）。 */
    @SubscribeEvent
    public static void registerBlockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0 || !state.hasProperty(com.piranport.dungeon.block.DungeonLecternBlock.PATTERN)) return -1;
            return state.getValue(com.piranport.dungeon.block.DungeonLecternBlock.PATTERN).tint();
        }, com.piranport.registry.ModBlocks.DUNGEON_LECTERN.get());
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.STONE_MILL_MENU.get(), StoneMillScreen::new);
        event.register(ModMenuTypes.COOKING_POT_MENU.get(), CookingPotScreen::new);
        event.register(ModMenuTypes.RELOAD_FACILITY_MENU.get(), ReloadFacilityScreen::new);
        // 弹药工作台
        event.register(ModMenuTypes.AMMO_WORKBENCH_MENU.get(), AmmoWorkbenchScreen::new);
        // 武器工作台
        event.register(ModMenuTypes.WEAPON_WORKBENCH_MENU.get(),
                com.piranport.menu.WeaponWorkbenchScreen::new);
        // 蓝图收纳箱
        event.register(ModMenuTypes.BLUEPRINT_CHEST_MENU.get(),
                com.piranport.menu.BlueprintChestScreen::new);
        // 调试终端
        event.register(ModMenuTypes.DEBUG_TERMINAL_MENU.get(),
                com.piranport.client.gui.DebugTerminalScreen::new);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.FIRE_CONTROL_SELECT);
        event.register(ModKeyMappings.TOGGLE_AUTO_LAUNCH);
        event.register(ModKeyMappings.MANUAL_RELOAD);
        event.register(ModKeyMappings.SWITCH_AMMO);
        event.register(ModKeyMappings.RECON_EXIT);
    }

    @SubscribeEvent
    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        // 武器装填条（武器物品上的耐久条样式）
        WeaponReloadDecorator weaponDecorator = new WeaponReloadDecorator();
        event.register(ModItems.MEDIUM_GUN.get(), weaponDecorator);
        // 07 火炮注册表 62 门（含日本12厘米单装炮、日本12.7厘米连装炮、德国双联380毫米炮）
        for (net.minecraft.world.item.Item gun : com.piranport.registry.WeaponItems.allCatalogGuns()) {
            event.register(gun, weaponDecorator);
        }
        event.register(ModItems.TRIPLE_TORPEDO_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.QUAD_TORPEDO_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.QUINTUPLE_TORPEDO_LAUNCHER.get(), weaponDecorator);
        ModItems.CATALOG_TORPEDO_LAUNCHERS.forEach(l -> event.register(l.get(), weaponDecorator));
        event.register(ModItems.DEPTH_CHARGE_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.DEPTH_CHARGE_LAUNCHER_IMPROVED.get(), weaponDecorator);
        event.register(ModItems.DEPTH_CHARGE_LAUNCHER_ADVANCED.get(), weaponDecorator);
        // 导弹发射器——所有类型都显示冷却条
        event.register(ModItems.TERRIER_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.SEA_DART_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.SEACAT_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.SY1_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.MK14_HARPOON_LAUNCHER.get(), weaponDecorator);
        event.register(ModItems.SHIP_ROCKET_LAUNCHER.get(), weaponDecorator);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skinModel : event.getSkins()) {
            var renderer = event.getSkin(skinModel);
            if (renderer instanceof PlayerRenderer playerRenderer) {
                playerRenderer.addLayer(
                        new com.piranport.client.SkinOverlayLayer(playerRenderer));
                playerRenderer.addLayer(
                        new com.piranport.client.ShipRiggingLayer(playerRenderer));
            }
        }
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(com.piranport.client.model.B25Model.LAYER_LOCATION,
                com.piranport.client.model.B25Model::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.F4FModel.LAYER_LOCATION,
                com.piranport.client.model.F4FModel::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.BarracudaModel.LAYER_LOCATION,
                com.piranport.client.model.BarracudaModel::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.RyuseiModel.LAYER_LOCATION,
                com.piranport.client.model.RyuseiModel::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.TengaModel.LAYER_LOCATION,
                com.piranport.client.model.TengaModel::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.TorpedoModel.LAYER_LOCATION,
                com.piranport.client.model.TorpedoModel::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.UnicornModel.LAYER_LOCATION,
                com.piranport.client.model.UnicornModel::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.KitchenGoddessModel.LAYER_LOCATION,
                com.piranport.client.model.KitchenGoddessModel::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.DeepOceanDestroyerModel.LAYER_LOCATION,
                com.piranport.client.model.DeepOceanDestroyerModel::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.DeepOceanSupplyModel.LAYER_LOCATION,
                com.piranport.client.model.DeepOceanSupplyModel::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.DeepOceanHeavyCruiserModel.LAYER_LOCATION,
                com.piranport.client.model.DeepOceanHeavyCruiserModel::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.DeepOceanSubmarineModel.LAYER_LOCATION,
                com.piranport.client.model.DeepOceanSubmarineModel::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.DeepOceanBattleCruiserModel.LAYER_LOCATION,
                com.piranport.client.model.DeepOceanBattleCruiserModel::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.DeepOceanBattleshipModel.LAYER_LOCATION,
                com.piranport.client.model.DeepOceanBattleshipModel::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.DeepOceanLightCarrierModel.LAYER_LOCATION,
                com.piranport.client.model.DeepOceanLightCarrierModel::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.Ju87cModel.LAYER_LOCATION,
                com.piranport.client.model.Ju87cModel::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.BtdDestroyerModel.LAYER_LOCATION,
                com.piranport.client.model.BtdDestroyerModel::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.WyvernModel.LAYER_LOCATION,
                com.piranport.client.model.WyvernModel::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.SkyraiderModel.LAYER_LOCATION,
                com.piranport.client.model.SkyraiderModel::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.F4u7Model.LAYER_LOCATION,
                com.piranport.client.model.F4u7Model::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.Type96FighterModel.LAYER_LOCATION,
                com.piranport.client.model.Type96FighterModel::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.F2aBuffaloModel.LAYER_LOCATION,
                com.piranport.client.model.F2aBuffaloModel::createBodyLayer);
        event.registerLayerDefinition(com.piranport.client.model.ReppuModel.LAYER_LOCATION,
                com.piranport.client.model.ReppuModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.CANNON_PROJECTILE.get(),
                CannonProjectileRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.TORPEDO_ENTITY.get(),
                TorpedoRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.AIRCRAFT_ENTITY.get(),
                AircraftRenderer::new);
        // 飞机被击落后掉落的物品实体（AircraftDropEntity extends ItemEntity）。
        // WHY：EntityRenderDispatcher.getRenderer 只按 entity.getType() 查表，原版 ItemEntity
        // 的渲染器挂在 minecraft:item 这个 EntityType 上，不会被子类类型继承；漏注册时
        // shouldRender 直接 NPE 崩客户端（"entityrenderer is null"）。
        event.registerEntityRenderer(ModEntityTypes.AIRCRAFT_DROP.get(),
                net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.FLOATING_TARGET.get(),
                net.minecraft.client.renderer.entity.ArmorStandRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.AERIAL_BOMB.get(),
                ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.BULLET.get(),
                ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.SANSHIKI_PELLET.get(),
                ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.DEPTH_CHARGE.get(),
                ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.MISSILE_ENTITY.get(),
                MissileProjectileRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.FLARE_PROJECTILE.get(),
                ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.RAILGUN_PROJECTILE.get(),
                ThrownItemRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.CUTTING_BOARD.get(),
                CuttingBoardRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.PLACEABLE_FOOD.get(),
                PlaceableFoodRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.MODEL_DEBUG.get(),
                com.piranport.client.ModelDebugBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.SMOKE_SCREEN.get(),
                com.piranport.client.SmokeScreenRenderer::new);
        // 副本书台：上方悬浮字幕（关卡名 / 通关情况 / 人数）
        event.registerBlockEntityRenderer(ModBlockEntityTypes.DUNGEON_LECTERN.get(),
                com.piranport.dungeon.client.DungeonLecternRenderer::new);
        // 副本敌人：低级驱逐舰也统一用 Alex 人形占位
        event.registerEntityRenderer(ModEntityTypes.LOW_TIER_DESTROYER.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        // 副本运输机（炮击开场脚本）
        event.registerEntityRenderer(ModEntityTypes.DUNGEON_TRANSPORT_PLANE.get(),
                com.piranport.client.DungeonTransportPlaneRenderer::new);
        // 副本出口传送门：实体只做隐形触发与多门去向文字，视觉主体是方块门框 + tick() 粒子
        event.registerEntityRenderer(ModEntityTypes.DUNGEON_PORTAL.get(),
                com.piranport.dungeon.client.DungeonPortalRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.LOOT_SHIP.get(),
                com.piranport.dungeon.client.LootShipRenderer::new);
        // 冈格尼尔
        event.registerEntityRenderer(ModEntityTypes.GUNGNIR.get(),
                ThrownItemRenderer::new);
        // 深海投射物
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_PROJECTILE.get(),
                ThrownItemRenderer::new);
        // 深海 NPC 实体：统一改为 Alex（slim）玩家模型 + 原版 Alex 皮肤占位。
        // 后续按舰种补皮肤时只改 AlexSlimMobRenderer#getTextureLocation，不再换模型。
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_SUPPLY.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_DESTROYER.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_LIGHT_CRUISER.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_HEAVY_CRUISER.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_BATTLE_CRUISER.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_BATTLESHIP.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_LIGHT_CARRIER.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_CARRIER.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_SUBMARINE.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_BOSS.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        // 深海扩展舰种：5 艘人形舰（装甲航母/雷巡/大凤/信浓/瑞鹤）沿用 Alex 占位。
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_ARMORED_CARRIER.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_TORPEDO_CRUISER.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_TAIHOU.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_SHINANO.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_ZUIKAKU.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        // 高级档与旗舰暂无独立模型，沿用同舰种低级的 Alex 占位
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_HIGH_DESTROYER.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_AIR_DEFENSE_DESTROYER.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_HIGH_AIR_DEFENSE_DESTROYER.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_HIGH_LIGHT_CRUISER.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_HIGH_HEAVY_CRUISER.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_HIGH_BATTLE_CRUISER.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_HIGH_BATTLESHIP.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_HIGH_LIGHT_CARRIER.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_HIGH_CARRIER.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_HIGH_TORPEDO_CRUISER.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_HIGH_SUBMARINE.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_FLAGSHIP.get(),
                ctx -> new AlexSlimMobRenderer<>(ctx));
        // 运输船是箱船，不使用人形模型，单独用方块船体渲染器。
        event.registerEntityRenderer(ModEntityTypes.DEEP_OCEAN_TRANSPORT.get(),
                DeepOceanTransportRenderer::new);
        // 金猫猫是活动关的猫，不是深海舰，保留原通用渲染器。
        event.registerEntityRenderer(ModEntityTypes.GOLDENCATCAT.get(),
                com.piranport.client.DeepOceanRenderer::new);
        // 舰娘 NPC：随从暂用艾利克斯（原版细手臂）模型占位，见 AlexSlimMobRenderer
        event.registerEntityRenderer(ModEntityTypes.SHIP_GIRL.get(),
                com.piranport.client.AlexSlimMobRenderer::new);
        // 平海：YSM 模型转成的 Bedrock 几何渲染器
        event.registerEntityRenderer(ModEntityTypes.PINGHAI.get(),
                com.piranport.client.PinghaiRenderer::new);
    }
}
