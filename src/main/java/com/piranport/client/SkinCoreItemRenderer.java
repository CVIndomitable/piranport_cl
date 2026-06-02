package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.piranport.PiranPort;
import com.piranport.item.SkinCoreItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Custom renderer for SkinCoreItem that displays a 3D player head
 * with the corresponding skin texture in the inventory.
 */
public class SkinCoreItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static SkinCoreItemRenderer INSTANCE;

    /**
     * IClientItemExtensions 实例供 SkinCoreItem.initializeClient() 使用。
     * getCustomRenderer() 返回渲染器单例，所有皮肤核心共用一个渲染器。
     */
    public static final IClientItemExtensions CLIENT_EXTENSIONS = new IClientItemExtensions() {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return SkinCoreItemRenderer.getInstance();
        }
    };

    // 延迟初始化：EntityModelSet 在资源加载时才填充，
    // FMLClientSetupEvent 阶段 roots 为空，直接 bakeLayer 会崩溃。
    private SkullModelBase headModel;

    public SkinCoreItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
    }

    /**
     * 延迟获取头颅模型。首次渲染时 EntityModelSet 已就绪，此时安全调用 bakeLayer。
     */
    private SkullModelBase getHeadModel() {
        if (this.headModel == null) {
            EntityModelSet modelSet = Minecraft.getInstance().getEntityModels();
            this.headModel = new SkullModel(modelSet.bakeLayer(ModelLayers.PLAYER_HEAD));
            PiranPort.LOGGER.info("SkinCoreItemRenderer head model initialized");
        }
        return this.headModel;
    }

    /**
     * 初始化渲染器单例。在 FMLClientSetupEvent 中调用，仅创建渲染器实例。
     * 头颅模型延迟到首次渲染时初始化（等待 EntityModelSet 就绪）。
     */
    public static void init() {
        PiranPort.LOGGER.info("SkinCoreItemRenderer initializing...");
        if (INSTANCE == null) {
            Minecraft mc = Minecraft.getInstance();
            INSTANCE = new SkinCoreItemRenderer(
                    mc.getBlockEntityRenderDispatcher(),
                    mc.getEntityModels()
            );
            PiranPort.LOGGER.info("SkinCoreItemRenderer singleton created (model deferred)");
        }
    }

    /**
     * 获取渲染器单例。调用前必须先调用 {@link #init()}，否则抛异常。
     */
    public static SkinCoreItemRenderer getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException(
                    "SkinCoreItemRenderer not initialized — call init() in FMLClientSetupEvent");
        }
        return INSTANCE;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                             PoseStack poseStack, MultiBufferSource bufferSource,
                             int packedLight, int packedOverlay) {
        if (!(stack.getItem() instanceof SkinCoreItem skinCore)) return;

        int skinId = skinCore.getSkinId();
        ResourceLocation skinTexture = ResourceLocation.fromNamespaceAndPath(
                PiranPort.MOD_ID, "textures/skin/skin_" + skinId + ".png");

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.25F, 0.5F);

        float scale = 0.625F;
        poseStack.scale(scale, -scale, -scale);

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(skinTexture));
        getHeadModel().renderToBuffer(poseStack, vc, packedLight, packedOverlay, -1);

        poseStack.popPose();
    }
}
