package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piranport.PiranPort;
import com.piranport.component.LoadedAmmo;
import com.piranport.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 图鉴鱼雷发射器的通用自定义物品渲染（BEWLR）。
 *
 * <p>同一套逻辑服务多具发射器：物品栏画 32x32 图标，第一/第三人称渲染对应
 * {@code models/item/<id>_3d.json} 的立体模型。每具发射器用 {@link Spec} 描述
 * 物品、图标和模型路径，避免为每具发射器重复一份 180 行渲染器。</p>
 *
 * <p>WHY：物品模型无法按显示上下文切换模型，只有 BEWLR 能同时做到
 * 「物品栏是图标、手持是立体发射器」。图标不依赖模型 JSON，直接绑定原贴图，
 * 装填/空膛两态在这里按 LOADED_AMMO 组件选择；3D 模型用两份 standalone 模型
 * （空膛态通常把管口/雷头换成暗色贴图）。</p>
 */
public class CatalogTorpedoLauncherRenderer extends BlockEntityWithoutLevelRenderer {

    /**
     * 一具发射器的渲染描述。
     *
     * @param id   资源名，用于拼图标与 3D 模型路径
     * @param items 使用同一套素材的全部物品（改型可共享）
     */
    public static final class Spec {
        private final String id;
        private final List<Supplier<? extends Item>> items;
        private final ResourceLocation iconLoaded;
        private final ResourceLocation iconEmpty;
        private final ModelResourceLocation modelEmpty;
        private final ModelResourceLocation modelLoaded;

        private Spec(String id, List<Supplier<? extends Item>> items) {
            this.id = id;
            this.items = items;
            this.iconLoaded = ResourceLocation.fromNamespaceAndPath(
                    PiranPort.MOD_ID, "textures/item/" + id + ".png");
            this.iconEmpty = ResourceLocation.fromNamespaceAndPath(
                    PiranPort.MOD_ID, "textures/item/" + id + "_empty.png");
            this.modelEmpty = ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID,
                            "item/" + id + "_3d"));
            this.modelLoaded = ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID,
                            "item/" + id + "_3d_loaded"));
        }

        public String id() {
            return id;
        }

        public List<Supplier<? extends Item>> items() {
            return items;
        }

        public ModelResourceLocation modelEmpty() {
            return modelEmpty;
        }

        public ModelResourceLocation modelLoaded() {
            return modelLoaded;
        }

        private boolean matches(ItemStack stack) {
            for (Supplier<? extends Item> item : items) {
                if (stack.is(item.get())) return true;
            }
            return false;
        }
    }

    private static final List<Spec> SPECS = new ArrayList<>();
    private static final Map<Spec, CatalogTorpedoLauncherRenderer> INSTANCES = new IdentityHashMap<>();

    private final Spec spec;

    private CatalogTorpedoLauncherRenderer(BlockEntityRenderDispatcher dispatcher,
                                           net.minecraft.client.model.geom.EntityModelSet modelSet,
                                           Spec spec) {
        super(dispatcher, modelSet);
        this.spec = spec;
    }

    /** 注册一份渲染描述；在 {@link ClientModEvents} 静态初始化时调用。 */
    @SafeVarargs
    public static Spec spec(String id, Supplier<? extends Item>... items) {
        Spec spec = new Spec(id, List.of(items));
        SPECS.add(spec);
        return spec;
    }

    public static List<Spec> specs() {
        return SPECS;
    }

    /** 在 FMLClientSetupEvent 里创建全部渲染器实例。 */
    public static synchronized void initAll() {
        Minecraft mc = Minecraft.getInstance();
        for (Spec spec : SPECS) {
            INSTANCES.computeIfAbsent(spec, s -> new CatalogTorpedoLauncherRenderer(
                    mc.getBlockEntityRenderDispatcher(), mc.getEntityModels(), s));
        }
    }

    public static IClientItemExtensions extensionsFor(Spec spec) {
        return new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                CatalogTorpedoLauncherRenderer renderer = INSTANCES.get(spec);
                if (renderer == null) {
                    initAll();
                    renderer = INSTANCES.get(spec);
                }
                if (renderer == null) {
                    throw new IllegalStateException(
                            "CatalogTorpedoLauncherRenderer not initialized for " + spec.id());
                }
                return renderer;
            }
        };
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource bufferSource,
                             int packedLight, int packedOverlay) {
        if (!spec.matches(stack)) return;

        if (isHandContext(displayContext)) {
            renderLauncher3D(stack, poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            renderFlatIcon(displayContext, isLoaded(stack), poseStack, bufferSource,
                    packedLight, packedOverlay);
        }
    }

    private static boolean isHandContext(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }

    private static boolean isLoaded(ItemStack stack) {
        LoadedAmmo ammo = stack.get(ModDataComponents.LOADED_AMMO.get());
        return ammo != null && ammo.hasAmmo();
    }

    /**
     * 渲染 3D 发射器。模型是 standalone 注册的，按 LOADED_AMMO 选空膛/装填版本。
     * 坐标空间：物品渲染管线已在 pose 中做过 translate(-0.5)，模型坐标即 0..1。
     */
    private void renderLauncher3D(ItemStack stack, PoseStack poseStack,
                                  MultiBufferSource bufferSource,
                                  int packedLight, int packedOverlay) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getModelManager().getModel(
                isLoaded(stack) ? spec.modelLoaded() : spec.modelEmpty());
        if (model == null || model == mc.getModelManager().getMissingModel()) return;

        VertexConsumer consumer = bufferSource.getBuffer(Sheets.translucentItemSheet());
        PoseStack.Pose pose = poseStack.last();
        RandomSource random = RandomSource.create(42L);

        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
            for (BakedQuad quad : model.getQuads(null, direction, random)) {
                consumer.putBulkData(pose, quad, 1.0F, 1.0F, 1.0F, 1.0F,
                        packedLight, packedOverlay);
            }
        }
        random.setSeed(42L);
        for (BakedQuad quad : model.getQuads(null, null, random)) {
            consumer.putBulkData(pose, quad, 1.0F, 1.0F, 1.0F, 1.0F,
                    packedLight, packedOverlay);
        }
    }

    /**
     * GUI/地面/展示框用的平面图标。
     * 坐标用方块空间 0..1（渲染管线已在 pose 里 translate(-0.5)，会把它居中）。
     * u/v 按物品图标习惯：左下 (0,1)、右上 (1,0)。
     */
    private void renderFlatIcon(ItemDisplayContext context, boolean loaded,
                                PoseStack poseStack, MultiBufferSource bufferSource,
                                int packedLight, int packedOverlay) {
        ResourceLocation texture = loaded ? spec.iconLoaded : spec.iconEmpty;
        // GUI 里用 emissive：图标按原色显示。entity 的 cutout 渲染会吃
        // gui 的两盏方向光，比原版 item shader 暗一档。
        RenderType type = context == ItemDisplayContext.GUI
                ? RenderType.entityTranslucentEmissive(texture)
                : RenderType.entityCutoutNoCull(texture);
        VertexConsumer consumer = bufferSource.getBuffer(type);
        PoseStack.Pose pose = poseStack.last();
        Vector3f normal = pose.transformNormal(0.0F, 0.0F, 1.0F, new Vector3f());

        iconVertex(consumer, pose, normal, 0.0F, 0.0F, 0.5F, 0.0F, 1.0F,
                packedLight, packedOverlay);
        iconVertex(consumer, pose, normal, 1.0F, 0.0F, 0.5F, 1.0F, 1.0F,
                packedLight, packedOverlay);
        iconVertex(consumer, pose, normal, 1.0F, 1.0F, 0.5F, 1.0F, 0.0F,
                packedLight, packedOverlay);
        iconVertex(consumer, pose, normal, 0.0F, 1.0F, 0.5F, 0.0F, 0.0F,
                packedLight, packedOverlay);
    }

    private static void iconVertex(VertexConsumer consumer, PoseStack.Pose pose,
                                   Vector3f normal, float x, float y, float z,
                                   float u, float v, int packedLight, int packedOverlay) {
        Vector3f pos = pose.pose().transformPosition(x, y, z, new Vector3f());
        consumer.addVertex(pos.x(), pos.y(), pos.z())
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(normal.x(), normal.y(), normal.z());
    }
}
