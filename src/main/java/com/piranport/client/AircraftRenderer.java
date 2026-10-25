package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.piranport.client.model.B25Model;
import com.piranport.client.model.ReppuModel;
import com.piranport.client.model.F2aBuffaloModel;
import com.piranport.client.model.Type96FighterModel;
import com.piranport.client.model.F4u7Model;
import com.piranport.client.model.SkyraiderModel;
import com.piranport.client.model.WyvernModel;
import com.piranport.client.model.BtdDestroyerModel;
import com.piranport.client.model.Ju87cModel;
import com.piranport.client.model.BarracudaModel;
import com.piranport.client.model.F4FModel;
import com.piranport.client.model.RyuseiModel;
import com.piranport.client.model.TengaModel;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.entity.AircraftEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Aircraft renderer using 3D entity models. The model and texture are chosen
 * from the entity's immutable aircraft definition visual id.
 */
public class AircraftRenderer extends EntityRenderer<AircraftEntity> {

    private final B25Model<AircraftEntity> b25Model;
    private final F4FModel<AircraftEntity> f4fModel;
    private final BarracudaModel<AircraftEntity> barracudaModel;
    private final RyuseiModel<AircraftEntity> ryuseiModel;
    private final TengaModel<AircraftEntity> tengaModel;

    private final Ju87cModel<AircraftEntity> ju87cModel;

    private final BtdDestroyerModel<AircraftEntity> btdDestroyerModel;

    private final WyvernModel<AircraftEntity> wyvernModel;

    private final SkyraiderModel<AircraftEntity> skyraiderModel;

    private final F4u7Model<AircraftEntity> f4u7Model;

    private final Type96FighterModel<AircraftEntity> type96FighterModel;

    private final F2aBuffaloModel<AircraftEntity> f2aBuffaloModel;

    private final ReppuModel<AircraftEntity> reppuModel;

    public AircraftRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.5f;
        this.b25Model = new B25Model<>(ctx.bakeLayer(B25Model.LAYER_LOCATION));
        this.f4fModel = new F4FModel<>(ctx.bakeLayer(F4FModel.LAYER_LOCATION));
        this.barracudaModel = new BarracudaModel<>(ctx.bakeLayer(BarracudaModel.LAYER_LOCATION));
        this.ryuseiModel = new RyuseiModel<>(ctx.bakeLayer(RyuseiModel.LAYER_LOCATION));
        this.tengaModel = new TengaModel<>(ctx.bakeLayer(TengaModel.LAYER_LOCATION));
        this.ju87cModel = new Ju87cModel<>(ctx.bakeLayer(Ju87cModel.LAYER_LOCATION));
        this.btdDestroyerModel = new BtdDestroyerModel<>(ctx.bakeLayer(BtdDestroyerModel.LAYER_LOCATION));
        this.wyvernModel = new WyvernModel<>(ctx.bakeLayer(WyvernModel.LAYER_LOCATION));
        this.skyraiderModel = new SkyraiderModel<>(ctx.bakeLayer(SkyraiderModel.LAYER_LOCATION));
        this.f4u7Model = new F4u7Model<>(ctx.bakeLayer(F4u7Model.LAYER_LOCATION));
        this.type96FighterModel = new Type96FighterModel<>(ctx.bakeLayer(Type96FighterModel.LAYER_LOCATION));
        this.f2aBuffaloModel = new F2aBuffaloModel<>(ctx.bakeLayer(F2aBuffaloModel.LAYER_LOCATION));
        this.reppuModel = new ReppuModel<>(ctx.bakeLayer(ReppuModel.LAYER_LOCATION));
    }

    @Override
    public void render(AircraftEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // Canonical MC entity rotation: YP(180 - yaw) with scale(-1, -1, 1).
        // The model is authored facing -Z (nose at Z=-5); this rotates the
        // nose to +Z at yaw=0 (entity facing south) and flips the upside-down
        // Blockbench Y/X convention.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw));
        float pitch = net.minecraft.util.Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        // Pitch flips sign because the model's forward (+Z after Y rotation) is
        // achieved via a 180° flip; a positive Minecraft xRot (nose-down) must
        // rotate the model's local nose downward.
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.scale(-1.5f, -1.5f, 1.5f);
        poseStack.translate(0.0, -1.501, 0.0);

        EntityModel<AircraftEntity> model;
        AircraftVisualRegistry.AircraftVisual visual = AircraftVisualRegistry.resolve(entity);
        ResourceLocation texture = visual.texture();
        model = switch (visual.modelKind()) {
            case B25 -> b25Model;
            case F4F -> f4fModel;
            case BARRACUDA -> barracudaModel;
            case RYUSEI -> ryuseiModel;
            case TENGA -> tengaModel;
            case JU87C -> ju87cModel;
            case BTD_DESTROYER -> btdDestroyerModel;
            case WYVERN -> wyvernModel;
            case SKYRAIDER -> skyraiderModel;
            case F4U7 -> f4u7Model;
            case TYPE96_FIGHTER -> type96FighterModel;
            case F2A_BUFFALO -> f2aBuffaloModel;
            case REPPU -> reppuModel;
        };

        VertexConsumer vertexConsumer = bufferSource.getBuffer(model.renderType(texture));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, -1);

        poseStack.popPose();

        // 鱼雷机平飞段：从「飞机水面投影点」连到「目标水面投影点」的水平预瞄线。
        // 线型/颜色复用玩家鱼雷发射器那套（2px、无深度测试、半透明），不再用细白线。
        // RenderType 的顶点在 entity 局部空间（原点在插值后的实体位置），
        // 所以世界坐标要减去插值实体位置。
        Vec3 aim = entity.getTorpedoAimPoint();
        if (aim != null) {
            double ex = net.minecraft.util.Mth.lerp(partialTick, entity.xo, entity.getX());
            double ey = net.minecraft.util.Mth.lerp(partialTick, entity.yo, entity.getY());
            double ez = net.minecraft.util.Mth.lerp(partialTick, entity.zo, entity.getZ());
            // 服务端同步的 aim.y 就是目标列的静止水面高度（0.875），
            // 不再做客户端 heightmap 另算，保证预瞄线贴在同一水平面上。
            float lineY = (float) (aim.y - ey + 0.02);
            int rgb = ModEquipmentConfig.PREDICTION_LINE_COLOR.get();
            int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
            poseStack.pushPose();
            VertexConsumer line = bufferSource.getBuffer(
                    FireControlVisualRenderer.torpedoAimLineType());
            PoseStack.Pose last = poseStack.last();
            addLineVertex(line, last, 0.0f, lineY, 0.0f, r, g, b);
            addLineVertex(line, last,
                    (float) (aim.x - ex), lineY, (float) (aim.z - ez), r, g, b);
            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void addLineVertex(VertexConsumer c, PoseStack.Pose pose,
                                      float x, float y, float z, int r, int g, int b) {
        // torpedoAimLineType 使用 POSITION_COLOR 格式，不需要 normal；alpha 与玩家预瞄线一致。
        c.addVertex(pose.pose(), x, y, z).setColor(r, g, b, 150);
    }

    @Override
    public ResourceLocation getTextureLocation(AircraftEntity entity) {
        return AircraftVisualRegistry.resolve(entity).texture();
    }
}
