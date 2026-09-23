package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.piranport.PiranPort;
import com.piranport.block.entity.ModelDebugBlockEntity;
import com.piranport.client.model.B25Model;
import com.piranport.client.model.DeepOceanHeavyCruiserModel;
import com.piranport.client.model.DeepOceanLightCarrierModel;
import com.piranport.client.model.F4FModel;
import com.piranport.client.model.TorpedoModel;
import com.piranport.registry.ModItems;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Static entity-model renderer for /ppd model_debug.
 *
 * <p>调试台的模型分两族，渲染路径完全不同：
 * <ul>
 *   <li><b>Java 实体模型</b>（b25 / f4f / heavy_cruiser / light_carrier / torpedo）——
 *       由 {@code bakeLayer} 烘焙，套用 {@link AircraftRenderer} 在 yaw=0/pitch=0 下的姿态变换，
 *       再用六块方向告示牌核对"机头指向哪个世界方向"。</li>
 *   <li><b>弹体</b>（shell / missile）——这些不是 Java 类建的模型，而是 Bedrock 风格 JSON，
 *       实际渲染由隐藏载体物品承载（见 {@code CannonProjectileRenderer}）。这里复用同一条路径：
 *       临时构造载体物品的 ItemStack 交给 {@link ItemRenderer}，缩放也用飞行中的那组常量，
 *       这样调试台看到的大小就是玩家在战场上看到的大小。</li>
 * </ul>
 */
public class ModelDebugBlockEntityRenderer implements BlockEntityRenderer<ModelDebugBlockEntity> {

    private static final ResourceLocation B25_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/b25.png");
    private static final ResourceLocation F4F_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/f4f.png");
    private static final ResourceLocation HEAVY_CRUISER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID,
                    "textures/entity/deep_ocean/heavy_cruiser_model.png");
    private static final ResourceLocation LIGHT_CARRIER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID,
                    "textures/entity/deep_ocean/light_carrier_model.png");
    private static final ResourceLocation TORPEDO_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "textures/entity/torpedo.png");

    /** 与 TorpedoRenderer 的视觉缩放保持一致，调试台所见即战场所见。 */
    private static final float TORPEDO_SCALE = 0.75f;

    // 弹体缩放照抄 CannonProjectileRenderer / MissileProjectileRenderer 的常量。
    // 刻意不共享常量类：那两处各自注释了策划口径的来由，调试台只是复现，改动源头仍是它们。
    private static final float SHELL_SMALL_SCALE = 0.75f;
    private static final float SHELL_MEDIUM_SCALE = 0.85f;
    private static final float SHELL_LARGE_SCALE = 0.95f;
    private static final float MISSILE_ANTI_AIR_SCALE = 0.75f;
    private static final float MISSILE_ANTI_SHIP_SCALE = 0.85f;
    private static final float MISSILE_ROCKET_SCALE = 0.75f;

    /**
     * BER 的渲染裁剪盒。
     *
     * <p>WHY 必须挂在渲染器上而不是 BlockEntity 上：NeoForge 查询的是
     * {@code IBlockEntityRendererExtension.getRenderBoundingBox}，即只有渲染器这一侧
     * 会被调用。模型几何在 yaw=180 / scale=-1.5 的变换下大约占据 1.5 格半径，
     * 加上上方的 B25 与六块方向告示牌，留 3 格余量。不开的话默认裁剪盒只有 1 格，
     * 玩家稍微偏头模型就会被剔掉（表现为"模型一闪一闪"）。
     */
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(ModelDebugBlockEntity be) {
        return new net.minecraft.world.phys.AABB(be.getBlockPos()).inflate(3.0);
    }

    private final B25Model<Entity> b25;
    private final F4FModel<Entity> f4f;
    private final DeepOceanHeavyCruiserModel heavyCruiser;
    private final DeepOceanLightCarrierModel lightCarrier;
    private final TorpedoModel torpedo;
    private final ItemRenderer itemRenderer;

    public ModelDebugBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.b25 = new B25Model<>(ctx.getModelSet().bakeLayer(B25Model.LAYER_LOCATION));
        this.f4f = new F4FModel<>(ctx.getModelSet().bakeLayer(F4FModel.LAYER_LOCATION));
        this.heavyCruiser = new DeepOceanHeavyCruiserModel(
                ctx.getModelSet().bakeLayer(DeepOceanHeavyCruiserModel.LAYER_LOCATION));
        this.lightCarrier = new DeepOceanLightCarrierModel(
                ctx.getModelSet().bakeLayer(DeepOceanLightCarrierModel.LAYER_LOCATION));
        this.torpedo = new TorpedoModel(ctx.getModelSet().bakeLayer(TorpedoModel.LAYER_LOCATION));
        this.itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(ModelDebugBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (renderProjectileItem(be, poseStack, bufferSource, packedLight)) {
            return;
        }

        EntityModel<?> model;
        ResourceLocation texture;
        switch (be.getModelType()) {
            case "f4f" -> { model = f4f; texture = F4F_TEXTURE; }
            case "heavy_cruiser" -> { model = heavyCruiser; texture = HEAVY_CRUISER_TEXTURE; }
            case "light_carrier" -> { model = lightCarrier; texture = LIGHT_CARRIER_TEXTURE; }
            case "torpedo" -> { model = torpedo; texture = TORPEDO_TEXTURE; }
            case "b25" -> { model = b25; texture = B25_TEXTURE; }
            default    -> { model = b25; texture = B25_TEXTURE; }
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        // Mirror AircraftRenderer transform with yaw=0 / pitch=0.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        poseStack.scale(-1.5f, -1.5f, 1.5f);
        poseStack.translate(0.0, -1.501, 0.0);

        VertexConsumer vc = bufferSource.getBuffer(model.renderType(texture));
        model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, -1);

        poseStack.popPose();
    }

    /**
     * 弹体渲染分支：命中 shell / missile 时渲染载体物品并返回 true。
     *
     * <p>姿态固定为"弹尖朝 +Z"（不套 Java 模型那套 yaw=180 翻转），
     * 让弹尖稳定指向 SOUTH 告示牌，便于比对模型本身的朝向；
     * 飞行中的实际定向由速度矢量决定，与这里无关。
     */
    private boolean renderProjectileItem(ModelDebugBlockEntity be, PoseStack poseStack,
                                         MultiBufferSource bufferSource, int packedLight) {
        ItemStack stack;
        float scale;
        switch (be.getModelType()) {
            case "shell" -> {
                switch (be.getVariant()) {
                    case "small" -> { stack = new ItemStack(ModItems.SHELL_PROJECTILE_SMALL.get()); scale = SHELL_SMALL_SCALE; }
                    case "large" -> { stack = new ItemStack(ModItems.SHELL_PROJECTILE_LARGE.get()); scale = SHELL_LARGE_SCALE; }
                    default      -> { stack = new ItemStack(ModItems.SHELL_PROJECTILE_MEDIUM.get()); scale = SHELL_MEDIUM_SCALE; }
                }
            }
            case "missile" -> {
                switch (be.getVariant()) {
                    case "anti_air" -> { stack = new ItemStack(ModItems.MISSILE_PROJECTILE_ANTI_AIR.get()); scale = MISSILE_ANTI_AIR_SCALE; }
                    case "rocket" -> { stack = new ItemStack(ModItems.MISSILE_PROJECTILE_ROCKET.get()); scale = MISSILE_ROCKET_SCALE; }
                    default        -> { stack = new ItemStack(ModItems.MISSILE_PROJECTILE_ANTI_SHIP.get()); scale = MISSILE_ANTI_SHIP_SCALE; }
                }
            }
            default -> {
                return false;
            }
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.scale(scale, scale, scale);
        // 用 NONE 而非 GROUND：显示变换会右乘在姿态之后，GROUND 自带的旋转/缩放会污染朝向。
        itemRenderer.renderStatic(stack, ItemDisplayContext.NONE, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, bufferSource, null,
                be.getBlockPos().hashCode());
        poseStack.popPose();
        return true;
    }
}
