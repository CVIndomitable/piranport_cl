package com.piranport.client.gecko;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piranport.PiranPort;
import com.piranport.client.AircraftLaunchPoseClientState;
import com.piranport.client.RiggingClientState;
import com.piranport.client.RiggingSocketTransforms;
import com.piranport.rigging.RiggingCategory;
import com.piranport.rigging.RiggingLoadout;
import com.piranport.rigging.RiggingMount;
import com.piranport.rigging.RiggingSocket;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.renderer.layer.vanilla.AttachedAnimatableRenderLayer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 玩家舰装船体的 GeckoLib 渲染层：通用舰体 / 航母舰体 / 飞行甲板 / 腰部鱼雷架。
 *
 * <p><b>模型选择</b>：
 * <ul>
 *   <li>基础舰体：{@link RiggingHullModelRegistry#GENERAL_HULL}；
 *       快捷栏有舰载机时改画 {@link RiggingHullModelRegistry#CARRIER_HULL}</li>
 *   <li>有舰载机时额外叠加 {@link RiggingHullModelRegistry#CARRIER_DECK}</li>
 *   <li>有腰部鱼雷挂点时叠加 {@link RiggingHullModelRegistry#WAIST_TORPEDO_RACK}</li>
 * </ul>
 *
 * <p><b>位姿</b>：与 {@link RiggingSocketTransforms#applyHullBase} 完全共用同一套
 * 「躯干 + 0.28/0.34 偏移 + 放飞后仰」，这样从旧程序化船体换到模型时位置不会跳；
 * 之后再做 GeckoLib 的上下翻转（{@code Axis.XP(180)}）、yawOffset 与缩放。
 *
 * <p><b>为什么按玩家 × 模型维护 animatable</b>：与 {@link RiggingAttachedLayer} 同理，
 * 父类缓存按 Level 只存一个实例，多个玩家会共用同一套动画状态而互相串。
 * 玩家离开世界后实体没有强引用，WeakHashMap 条目可被 GC 回收。
 */
public class RiggingHullLayer extends AttachedAnimatableRenderLayer<
        RiggingAnimatable,
        AbstractClientPlayer,
        PlayerModel<AbstractClientPlayer>,
        GeoObjectRenderer<RiggingAnimatable>> {

    private final RiggingObjectRenderer generalHullRenderer;
    private final RiggingObjectRenderer carrierHullRenderer;
    private final RiggingObjectRenderer carrierDeckRenderer;
    private final RiggingObjectRenderer waistRackRenderer;

    private final Map<AbstractClientPlayer, HullAnimatables> animatables = new WeakHashMap<>();

    /** 已经警告过的缺失模型，避免每帧刷日志。只在渲染线程读写。 */
    private final Set<ResourceLocation> reportedMissingModels = new HashSet<>();

    public RiggingHullLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        // 父类的工厂缓存不会被用到（下面重写了 getAnimatableInstance），只是为了满足构造参数。
        super(parent, level -> new RiggingAnimatable(RiggingHullModelRegistry.GENERAL_HULL));
        this.generalHullRenderer =
                new RiggingObjectRenderer(RiggingHullModelRegistry.GENERAL_HULL);
        this.carrierHullRenderer =
                new RiggingObjectRenderer(RiggingHullModelRegistry.CARRIER_HULL);
        this.carrierDeckRenderer =
                new RiggingObjectRenderer(RiggingHullModelRegistry.CARRIER_DECK);
        this.waistRackRenderer =
                new RiggingObjectRenderer(RiggingHullModelRegistry.WAIST_TORPEDO_RACK);
    }

    @Override
    public RiggingAnimatable getAnimatableInstance(AbstractClientPlayer player) {
        return animatablesFor(player).generalHull();
    }

    @Override
    protected boolean shouldRender(AbstractClientPlayer player) {
        if (player.isInvisible()) return false;
        if (!RiggingClientState.isRenderedAsTransformed(player)) return false;
        // EMPTY 是「未变身 / 还没有快照」的哨兵；已变身但快捷栏没武器仍然要画空船体。
        return !RiggingClientState.resolveLoadout(player).isEmpty();
    }

    @Override
    protected GeoObjectRenderer<RiggingAnimatable> getRenderer(RiggingAnimatable animatable) {
        // 父类只要求返回非空；实际每个模型用的是 renderAnimatableOnModel 里的专属 renderer。
        return generalHullRenderer;
    }

    @Override
    protected void renderAnimatableOnModel(AbstractClientPlayer player, RiggingAnimatable animatable,
                                           PlayerModel<AbstractClientPlayer> parentModel,
                                           GeoObjectRenderer<RiggingAnimatable> renderer,
                                           PoseStack poseStack, MultiBufferSource bufferSource,
                                           float partialTick, int packedLight,
                                           float ageInTicks, float limbSwing, float limbSwingAmount,
                                           float netHeadYaw, float headPitch) {
        RiggingLoadout loadout = RiggingClientState.resolveLoadout(player);
        float launchPose = AircraftLaunchPoseClientState.intensity(player.getId(), partialTick);
        HullAnimatables set = animatablesFor(player);

        // 1. 基础舰体：有舰载机时右臂去掉、换成航母舰体。
        if (loadout.hasAircraft()) {
            renderModel(poseStack, bufferSource, packedLight, partialTick, parentModel,
                    RiggingHullModelRegistry.CARRIER_HULL, set.carrierHull(),
                    carrierHullRenderer, launchPose);
        } else {
            renderModel(poseStack, bufferSource, packedLight, partialTick, parentModel,
                    RiggingHullModelRegistry.GENERAL_HULL, set.generalHull(),
                    generalHullRenderer, launchPose);
        }

        // 2. 飞行甲板：只在有舰载机时叠加。
        if (loadout.hasAircraft()) {
            renderModel(poseStack, bufferSource, packedLight, partialTick, parentModel,
                    RiggingHullModelRegistry.CARRIER_DECK, set.carrierDeck(),
                    carrierDeckRenderer, launchPose);
        }

        // 3. 腰部鱼雷架：任一挂点是腰部或鱼雷类别时叠加。
        if (hasWaistTorpedoMount(loadout)) {
            renderModel(poseStack, bufferSource, packedLight, partialTick, parentModel,
                    RiggingHullModelRegistry.WAIST_TORPEDO_RACK, set.waistRack(),
                    waistRackRenderer, launchPose);
        }
    }

    /**
     * 画一个船体部件。
     *
     * <p>先进入躯干参考系并加上与旧程序化船体一致的共享偏移，再翻转到 GeckoLib 的
     * +Y 朝上坐标系。{@code yawOffset} / {@code scale} 目前都是 0 / 1.0，保留参数
     * 是为了将来换模型时不用再改渲染代码。
     */
    private void renderModel(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                             float partialTick, PlayerModel<AbstractClientPlayer> parentModel,
                             RiggingHullModelDef def, RiggingAnimatable animatable,
                             RiggingObjectRenderer renderer, float launchPose) {
        // 资产缺失时跳过而不是崩客户端；只警告一次，避免每帧刷日志。
        if (!GeckoLibCache.getBakedModels().containsKey(def.geoModel())) {
            if (reportedMissingModels.add(def.geoModel())) {
                PiranPort.LOGGER.warn("舰装船体模型未加载，已跳过渲染: {}", def.geoModel());
            }
            return;
        }

        poseStack.pushPose();
        RiggingSocketTransforms.applyHullBase(poseStack, parentModel, launchPose);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(def.yawOffset()));
        poseStack.scale(def.scale(), def.scale(), def.scale());
        renderer.render(poseStack, animatable, bufferSource, null, null, packedLight, partialTick);
        poseStack.popPose();
    }

    private HullAnimatables animatablesFor(AbstractClientPlayer player) {
        return animatables.computeIfAbsent(player, ignored -> new HullAnimatables(
                new RiggingAnimatable(RiggingHullModelRegistry.GENERAL_HULL),
                new RiggingAnimatable(RiggingHullModelRegistry.CARRIER_HULL),
                new RiggingAnimatable(RiggingHullModelRegistry.CARRIER_DECK),
                new RiggingAnimatable(RiggingHullModelRegistry.WAIST_TORPEDO_RACK)));
    }

    /** 鱼雷挂点判断：socket 已是 WAIST_*，或类别是 TORPEDO（兼容旧快照里的 LEG_*）。 */
    private static boolean hasWaistTorpedoMount(RiggingLoadout loadout) {
        for (RiggingMount mount : loadout.mounts()) {
            if (mount.category() == RiggingCategory.TORPEDO) return true;
            if (mount.socket() == RiggingSocket.WAIST_LEFT
                    || mount.socket() == RiggingSocket.WAIST_RIGHT) {
                return true;
            }
        }
        return false;
    }

    /** 每个玩家 × 每个船体部件一份动画状态，避免多人动画串台。 */
    private record HullAnimatables(
            RiggingAnimatable generalHull,
            RiggingAnimatable carrierHull,
            RiggingAnimatable carrierDeck,
            RiggingAnimatable waistRack) {}
}
