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

    private final SkullModelBase headModel;

    public SkinCoreItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
        this.headModel = new SkullModel(modelSet.bakeLayer(ModelLayers.PLAYER_HEAD));
    }

    /**
     * 初始化渲染器单例。必须在 FMLClientSetupEvent 中显式调用，
     * 避免在模型烘焙等阶段因 EntityModelSet 未就绪而崩溃。
     */
    public static void init() {
        PiranPort.LOGGER.info("SkinCoreItemRenderer initializing...");
        if (INSTANCE == null) {
            Minecraft mc = Minecraft.getInstance();
            INSTANCE = new SkinCoreItemRenderer(
                    mc.getBlockEntityRenderDispatcher(),
                    mc.getEntityModels()
            );
            PiranPort.LOGGER.info("SkinCoreItemRenderer initialized with model: {}",
                    INSTANCE.headModel.getClass().getName());
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
        this.headModel.renderToBuffer(poseStack, vc, packedLight, packedOverlay, -1);

        poseStack.popPose();
    }
}
