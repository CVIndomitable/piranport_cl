package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piranport.PiranPort;
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
 * 日本12.7厘米连装炮的自定义物品渲染。
 *
 * <p>渲染分两种上下文：
 * <ul>
 *   <li>第一/第三人称：渲染 models/item/japanese_127mm_twin_gun_3d.json 的 3D 炮塔；</li>
 *   <li>GUI/掉落物/展示框等：直接画 32x32 物品图标，保持普通物品的外观。</li>
 * </ul>
 *
 * <p>WHY：物品模型无法按显示上下文切换模型，只有 BEWLR 能同时做到
 * 「物品栏是图标、手持是立体炮塔」。图标不依赖模型 JSON，直接绑定原贴图，
 * 装填/空膛两态在这里按 LOADED_AMMO 组件选择。
 */
public class Japanese127mmTwinGunRenderer extends BlockEntityWithoutLevelRenderer {

    private static Japanese127mmTwinGunRenderer INSTANCE;

    private static final ResourceLocation ICON_LOADED = ResourceLocation.fromNamespaceAndPath(
            PiranPort.MOD_ID, "textures/item/japanese_127mm_twin_gun.png");
    private static final ResourceLocation ICON_EMPTY = ResourceLocation.fromNamespaceAndPath(
            PiranPort.MOD_ID, "textures/item/japanese_127mm_twin_gun_empty.png");

    /** 两份 standalone 3D 模型，在 ModelEvent.RegisterAdditional 里注册烘焙。 */
    public static final ModelResourceLocation MODEL_EMPTY = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID,
                    "item/japanese_127mm_twin_gun_3d"));
    public static final ModelResourceLocation MODEL_LOADED = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID,
                    "item/japanese_127mm_twin_gun_3d_loaded"));

    public static final IClientItemExtensions CLIENT_EXTENSIONS = new IClientItemExtensions() {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return getInstance();
        }
    };

    public Japanese127mmTwinGunRenderer(BlockEntityRenderDispatcher dispatcher,
                                        net.minecraft.client.model.geom.EntityModelSet modelSet) {
        super(dispatcher, modelSet);
    }

    public static void init() {
        if (INSTANCE == null) {
            Minecraft mc = Minecraft.getInstance();
            INSTANCE = new Japanese127mmTwinGunRenderer(
                    mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
        }
    }

    public static Japanese127mmTwinGunRenderer getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException(
                    "Japanese127mmTwinGunRenderer not initialized — call init() in FMLClientSetupEvent");
        }
        return INSTANCE;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource bufferSource,
                             int packedLight, int packedOverlay) {
        if (!stack.is(ModItems.JAPANESE_127MM_TWIN_GUN.get())) return;

        if (isHandContext(displayContext)) {
            renderGun3D(stack, poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            renderFlatIcon(isLoaded(stack), poseStack, bufferSource, packedLight, packedOverlay);
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
     * 渲染 3D 炮塔。模型是 standalone 注册的，按 LOADED_AMMO 选空膛/装填版本。
     * 坐标空间：物品渲染管线已在 pose 中做过 translate(-0.5)，模型坐标即 0..1。
     */
    private static void renderGun3D(ItemStack stack, PoseStack poseStack,
                                    MultiBufferSource bufferSource,
                                    int packedLight, int packedOverlay) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getModelManager().getModel(
                isLoaded(stack) ? MODEL_LOADED : MODEL_EMPTY);
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
     * GUI/地面/展示框用的平面图标。模型空间 -0.5..0.5，z=0 平面。
     * u/v 按物品图标习惯：左下 (0,1)、右上 (1,0)。
     */
    private static void renderFlatIcon(boolean loaded, PoseStack poseStack,
                                       MultiBufferSource bufferSource,
                                       int packedLight, int packedOverlay) {
        ResourceLocation texture = loaded ? ICON_LOADED : ICON_EMPTY;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        PoseStack.Pose pose = poseStack.last();
        Vector3f normal = pose.transformNormal(0.0F, 0.0F, 1.0F, new Vector3f());

        iconVertex(consumer, pose, normal, -0.5F, -0.5F, 0.0F, 0.0F, 1.0F,
                packedLight, packedOverlay);
        iconVertex(consumer, pose, normal, 0.5F, -0.5F, 0.0F, 1.0F, 1.0F,
                packedLight, packedOverlay);
        iconVertex(consumer, pose, normal, 0.5F, 0.5F, 0.0F, 1.0F, 0.0F,
                packedLight, packedOverlay);
        iconVertex(consumer, pose, normal, -0.5F, 0.5F, 0.0F, 0.0F, 0.0F,
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
