package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.AircraftDefinitionService;
import com.piranport.client.model.B25Model;
import com.piranport.client.model.BarracudaModel;
import com.piranport.client.model.BtdDestroyerModel;
import com.piranport.client.model.F2aBuffaloModel;
import com.piranport.client.model.F4FModel;
import com.piranport.client.model.F4u7Model;
import com.piranport.client.model.Ju87cModel;
import com.piranport.client.model.ReppuModel;
import com.piranport.client.model.RyuseiModel;
import com.piranport.client.model.SeafangModel;
import com.piranport.client.model.SkyraiderModel;
import com.piranport.client.model.TengaModel;
import com.piranport.client.model.Type96FighterModel;
import com.piranport.client.model.WyvernModel;
import com.piranport.client.model.Xf5uModel;
import com.piranport.entity.AircraftEntity;
import com.piranport.rigging.RiggingSocket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/**
 * 舰装甲板上的飞机 3D 渲染：复用飞行实体的 {@link EntityModel}，不再画平面物品图标。
 *
 * <p><b>调用方坐标系</b>（与 {@code RiggingHullLayer} 里 GeckoLib 船体/甲板的坐标系一致）：
 * +Y 朝上、+Z 船首/前、+X 左舷，单位 = 格；原点 = 飞机落点（机腹贴原点平面）。
 * 第三人称调用前先 {@code applyHullBase} 再 {@code scale(1, -1, 1)}；
 * 第一人称的世界轴栈本身已经是这个坐标系，不要再翻 Y。
 *
 * <p><b>模型原始朝向</b>：EntityModel 空间沿用原版约定，+Y 朝下、{@code PartPose.y = 24}
 * 是地面；机头在 -Z。要落到「+Y 上、机头 +Z」的甲板系，需要绕 X 转 180°：
 * 模型 -Y（视觉上方）→ +Y，机头 -Z → +Z，模型 +X 仍是 +X（正常旋转，不镜像左右）。
 * 再沿模型 Y 下沉 1.5 格，把 {@code PartPose.y = 24} 的地面平面压到落点 y=0。
 *
 * <p><b>模型只烘焙一份</b>：15 个 EntityModel 按 {@link AircraftVisualRegistry.ModelKind}
 * 缓存在静态表里，所有玩家、所有甲板飞机共用。
 *
 * <p><b>延迟烘焙的原因</b>：{@code FMLClientSetupEvent} 阶段 {@code EntityModelSet} 还没有
 * 层定义（与 {@code SkinCoreItemRenderer} 相同），真正的 bake 放在实体渲染器注册
 * （此时 {@link net.minecraft.client.renderer.entity.EntityRendererProvider.Context#getModelSet()}
 * 已可用）或首次渲染时。
 */
public final class AircraftRiggingModelRenderer {

    /**
     * 甲板飞机缩放：最大机翼是 Tenga 的 18 单位，乘 0.38 后约 6.8 单位，
     * 能完整落在宽 7 单位的飞行甲板内（甲板 x -18.5..-11.5，中心 -15）。
     */
    public static final float DECK_SCALE = 0.38f;

    /** EntityModel 空间里 {@code PartPose.y = 24} 对应的地面高度（格）。 */
    private static final float MODEL_GROUND_Y = 1.5f;

    /**
     * carrier_deck 甲板顶面在舰装模型坐标系里的 y（格）。
     * Geo 模型里是 y=-0.8，但 GeckoLib/挂点都按 1 单位 = 1/16 格换算，所以是 -0.05 格。
     */
    private static final float DECK_TOP_Y = -0.8f / 16.0f;

    /** 抬高一点避免机腹与甲板顶面共面闪烁。 */
    private static final float DECK_CLEARANCE = 0.01f;

    private static Map<AircraftVisualRegistry.ModelKind, EntityModel<AircraftEntity>> models;

    private AircraftRiggingModelRenderer() {}

    /**
     * 烘焙并缓存 15 个飞机模型。重复调用无副作用。
     *
     * <p>只在客户端主线程调用；{@code EntityModelSet} 必须有层定义（注册实体渲染器之后）。
     */
    public static void init(EntityModelSet modelSet) {
        if (models != null) return;
        Map<AircraftVisualRegistry.ModelKind, EntityModel<AircraftEntity>> baked =
                new EnumMap<>(AircraftVisualRegistry.ModelKind.class);
        baked.put(AircraftVisualRegistry.ModelKind.B25,
                new B25Model<>(modelSet.bakeLayer(B25Model.LAYER_LOCATION)));
        baked.put(AircraftVisualRegistry.ModelKind.F4F,
                new F4FModel<>(modelSet.bakeLayer(F4FModel.LAYER_LOCATION)));
        baked.put(AircraftVisualRegistry.ModelKind.BARRACUDA,
                new BarracudaModel<>(modelSet.bakeLayer(BarracudaModel.LAYER_LOCATION)));
        baked.put(AircraftVisualRegistry.ModelKind.RYUSEI,
                new RyuseiModel<>(modelSet.bakeLayer(RyuseiModel.LAYER_LOCATION)));
        baked.put(AircraftVisualRegistry.ModelKind.TENGA,
                new TengaModel<>(modelSet.bakeLayer(TengaModel.LAYER_LOCATION)));
        baked.put(AircraftVisualRegistry.ModelKind.JU87C,
                new Ju87cModel<>(modelSet.bakeLayer(Ju87cModel.LAYER_LOCATION)));
        baked.put(AircraftVisualRegistry.ModelKind.BTD_DESTROYER,
                new BtdDestroyerModel<>(modelSet.bakeLayer(BtdDestroyerModel.LAYER_LOCATION)));
        baked.put(AircraftVisualRegistry.ModelKind.WYVERN,
                new WyvernModel<>(modelSet.bakeLayer(WyvernModel.LAYER_LOCATION)));
        baked.put(AircraftVisualRegistry.ModelKind.SKYRAIDER,
                new SkyraiderModel<>(modelSet.bakeLayer(SkyraiderModel.LAYER_LOCATION)));
        baked.put(AircraftVisualRegistry.ModelKind.F4U7,
                new F4u7Model<>(modelSet.bakeLayer(F4u7Model.LAYER_LOCATION)));
        baked.put(AircraftVisualRegistry.ModelKind.TYPE96_FIGHTER,
                new Type96FighterModel<>(modelSet.bakeLayer(Type96FighterModel.LAYER_LOCATION)));
        baked.put(AircraftVisualRegistry.ModelKind.F2A_BUFFALO,
                new F2aBuffaloModel<>(modelSet.bakeLayer(F2aBuffaloModel.LAYER_LOCATION)));
        baked.put(AircraftVisualRegistry.ModelKind.REPPU,
                new ReppuModel<>(modelSet.bakeLayer(ReppuModel.LAYER_LOCATION)));
        baked.put(AircraftVisualRegistry.ModelKind.SEAFANG,
                new SeafangModel<>(modelSet.bakeLayer(SeafangModel.LAYER_LOCATION)));
        baked.put(AircraftVisualRegistry.ModelKind.XF5U,
                new Xf5uModel<>(modelSet.bakeLayer(Xf5uModel.LAYER_LOCATION)));
        models = baked;
    }

    /**
     * 在调用方给定的落点（+Y 上、+Z 前）画一架平放的飞机，机腹贴 y=0、机头朝 +Z。
     *
     * <p>不调用 {@code setupAnim}：LayerDefinition 的静态姿态就是平飞姿态，甲板展示足够；
     * 也因此不需要构造假的 {@link AircraftEntity}。
     *
     * @return false 表示物品不是可识别的飞机（客户端还没拿到定义），没有画任何东西；
     *         调用方可据此回退到旧的物品图标。
     */
    public static boolean render(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource,
                                 int packedLight, float scale) {
        AircraftDefinition definition = AircraftDefinitionService.resolve(stack);
        if (definition == null) return false;
        AircraftVisualRegistry.AircraftVisual visual =
                AircraftVisualRegistry.resolve(definition.visualId());
        EntityModel<AircraftEntity> model = models().get(visual.modelKind());
        if (model == null) return false;

        poseStack.pushPose();
        // 模型 +Y 朝下、机头 -Z → 甲板系 +Y 朝上、机头 +Z。绕 X 180° 是正常旋转，
        // 左右（模型 +X）保持不变，不会镜像涂装。
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
        poseStack.scale(scale, scale, scale);
        // 先平移再缩放：把 PartPose.y=24（1.5 格）的地面压到落点 y=0。
        poseStack.translate(0.0f, -MODEL_GROUND_Y, 0.0f);

        VertexConsumer consumer = bufferSource.getBuffer(model.renderType(visual.texture()));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();
        return true;
    }

    /**
     * 把 poseStack 移到 DECK_1 / DECK_2 的飞机落点。
     *
     * <p>调用方必须已经在「+Y 上、+Z 船首、+X 左舷」的舰装模型坐标系里。
     * x/z 取 {@link RiggingSocketTransforms#mountPose} 的甲板挂点，y 用甲板顶面：
     * 挂点自带的 {@code pitch=-90} 和 y=0.4 是给平面物品图标用的，3D 飞机不能套。
     */
    public static void translateToDeck(PoseStack poseStack, RiggingSocket socket) {
        RiggingSocketTransforms.MountPose pose =
                RiggingSocketTransforms.mountPose(socket, 0.0f);
        poseStack.translate(pose.x() / 16.0f, DECK_TOP_Y + DECK_CLEARANCE, pose.z() / 16.0f);
    }

    private static Map<AircraftVisualRegistry.ModelKind, EntityModel<AircraftEntity>> models() {
        if (models == null) {
            // 正常路径由客户端注册实体渲染器时 init()；这里只兜底，保证不会因为接线漏掉而空甲板。
            init(Minecraft.getInstance().getEntityModels());
        }
        return models;
    }
}
