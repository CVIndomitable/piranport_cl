package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piranport.PiranPort;
import com.piranport.client.gecko.RiggingRenderContext;
import com.piranport.component.LoadedAmmo;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.joml.Vector3f;

/**
 * 日本12厘米单装炮的自定义物品渲染。
 *
 * <p>双手渲染带握把的模型，舰装挂点使用带回转座的模型，其他场合保留 32x32 原图标。
 *
 * <p>展示框与舰装都用 FIXED，需要舰装渲染作用域才能区分。两套模型和图标均保留装填/空膛两态。
 */
public class Japanese12cmSingleGunRenderer extends BlockEntityWithoutLevelRenderer {

    private static Japanese12cmSingleGunRenderer INSTANCE;

    private static final ResourceLocation ICON_LOADED = ResourceLocation.fromNamespaceAndPath(
            PiranPort.MOD_ID, "textures/item/single_small_gun.png");
    private static final ResourceLocation ICON_EMPTY = ResourceLocation.fromNamespaceAndPath(
            PiranPort.MOD_ID, "textures/item/single_small_gun_empty.png");

    /** 由 ModelEvent.RegisterAdditional 注册烘焙。 */
    public static final ModelResourceLocation MODEL_EMPTY = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID,
                    "item/single_small_gun_3d"));
    public static final ModelResourceLocation MODEL_LOADED = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID,
                    "item/single_small_gun_3d_loaded"));
    public static final ModelResourceLocation HELD_EMPTY = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "item/single_small_gun_held"));
    public static final ModelResourceLocation HELD_LOADED = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "item/single_small_gun_held_loaded"));

    public static final IClientItemExtensions CLIENT_EXTENSIONS = new IClientItemExtensions() {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return getInstance();
        }
    };

    public Japanese12cmSingleGunRenderer(BlockEntityRenderDispatcher dispatcher,
                                         net.minecraft.client.model.geom.EntityModelSet modelSet) {
        super(dispatcher, modelSet);
    }

    public static void init() {
        if (INSTANCE == null) {
            Minecraft mc = Minecraft.getInstance();
            INSTANCE = new Japanese12cmSingleGunRenderer(
                    mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
            PiranPort.LOGGER.info("Japanese12cmSingleGunRenderer initialized");
        }
    }

    public static Japanese12cmSingleGunRenderer getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException(
                    "Japanese12cmSingleGunRenderer not initialized — call init() in FMLClientSetupEvent");
        }
        return INSTANCE;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource bufferSource,
                             int packedLight, int packedOverlay) {
        if (!stack.is(ModItems.SINGLE_SMALL_GUN.get())) return;

        boolean loaded = isLoaded(stack);
        if (displayContext == ItemDisplayContext.FIXED && RiggingRenderContext.isRiggingPass()) {
            if (renderGun3D(loaded ? MODEL_LOADED : MODEL_EMPTY, poseStack, bufferSource, packedLight, packedOverlay)) {
                return;
            }
        } else if (displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            if (renderGun3D(loaded ? HELD_LOADED : HELD_EMPTY, poseStack, bufferSource, packedLight, packedOverlay)) {
                return;
            }
        }
        renderFlatIcon(displayContext, loaded, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static boolean isLoaded(ItemStack stack) {
        LoadedAmmo ammo = stack.get(ModDataComponents.LOADED_AMMO.get());
        return ammo != null && ammo.hasAmmo();
    }

    /**
     * 渲染 3D 炮塔。模型是 standalone 注册的，按 LOADED_AMMO 选空膛/装填版本。
     * 坐标空间：物品渲染管线已在 pose 中做过 translate(-0.5)，模型坐标即 0..1。
     */
    private static boolean renderGun3D(ModelResourceLocation resource, PoseStack poseStack,
                                       MultiBufferSource bufferSource,
                                       int packedLight, int packedOverlay) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getModelManager().getModel(resource);
        if (model == null || model == mc.getModelManager().getMissingModel()) return false;

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
        return true;
    }

    /**
     * GUI/地面/展示框用的平面图标。
     * 坐标用方块空间 0..1（渲染管线已在 pose 里 translate(-0.5)，会把它居中）。
     * u/v 按物品图标习惯：左下 (0,1)、右上 (1,0)。
     */
    private static void renderFlatIcon(ItemDisplayContext context, boolean loaded,
                                       PoseStack poseStack, MultiBufferSource bufferSource,
                                       int packedLight, int packedOverlay) {
        ResourceLocation texture = loaded ? ICON_LOADED : ICON_EMPTY;
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
