package com.piranport.client.gecko;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piranport.client.AircraftLaunchPoseClientState;
import com.piranport.client.RiggingClientState;
import com.piranport.client.RiggingSocketTransforms;
import com.piranport.rigging.RiggingLoadout;
import com.piranport.rigging.RiggingMount;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.renderer.layer.vanilla.AttachedAnimatableRenderLayer;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 玩家舰装 GeckoLib 渲染层：一个模型一个 layer 实例。
 *
 * <p><b>为什么按玩家维护 animatable</b>：{@code AttachedAnimatableRenderLayer} 的父类缓存
 * （字段 {@code instanceCache}）按 Level 只存一个弱引用实例，所有玩家共用同一套动画状态；
 * 本阶段只播 idle 还看不出来，但下一阶段接按玩家触发的开火动画时会互相串。
 * 所以这里重写 {@link #getAnimatableInstance}，用 WeakHashMap 给每个玩家一份。
 * 玩家离开世界后实体没有强引用，条目可被 GC 回收。
 *
 * <p><b>只渲染已变身且确实装了本模型的挂点</b>：{@link #shouldRender} 提前过滤，
 * 避免给旁观玩家每帧创建动画状态。
 */
public class RiggingAttachedLayer extends AttachedAnimatableRenderLayer<
        RiggingAnimatable,
        AbstractClientPlayer,
        PlayerModel<AbstractClientPlayer>,
        GeoObjectRenderer<RiggingAnimatable>> {

    private final RiggingModelDef def;
    private final RiggingObjectRenderer renderer;
    private final Map<AbstractClientPlayer, RiggingAnimatable> animatables = new WeakHashMap<>();

    public RiggingAttachedLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
            RiggingModelDef def) {
        // 父类的工厂缓存不会被用到（下面重写了 getAnimatableInstance），只是为了满足构造参数。
        super(parent, level -> new RiggingAnimatable(def));
        this.def = def;
        this.renderer = new RiggingObjectRenderer(def);
    }

    @Override
    public RiggingAnimatable getAnimatableInstance(AbstractClientPlayer player) {
        RiggingAnimatable animatable = animatables.get(player);
        if (animatable == null) {
            animatable = new RiggingAnimatable(def);
            animatables.put(player, animatable);
        }
        return animatable;
    }

    @Override
    protected boolean shouldRender(AbstractClientPlayer player) {
        if (player.isInvisible()) return false;
        if (!RiggingClientState.isRenderedAsTransformed(player)) return false;
        return hasMountOfThisModel(RiggingClientState.resolveLoadout(player));
    }

    @Override
    protected GeoObjectRenderer<RiggingAnimatable> getRenderer(RiggingAnimatable animatable) {
        return renderer;
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

        for (RiggingMount mount : loadout.mounts()) {
            if (!isThisModel(mount)) continue;

            poseStack.pushPose();
            // 1. 进入躯干/腿参考系，并加上舰体共享偏移与放飞后仰
            RiggingSocketTransforms.applyBase(poseStack, parentModel, mount.socket(), launchPose);
            // 2. 挂点偏移
            RiggingSocketTransforms.MountPose pose =
                    RiggingSocketTransforms.mountPose(mount.socket(), launchPose);
            RiggingSocketTransforms.applyMountOffset(poseStack, pose);

            // 3. GeckoLib 模型 +Y 朝上，玩家模型参考系 +Y 朝下：先绕 X 翻 180°。
            //    该翻转同时把模型 +Z 变成 -Z，所以 yawOffset 才能按「+Z 模型填 180」统一。
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
            // 4. 模型自身前后朝向
            poseStack.mulPose(Axis.YP.rotationDegrees(def.yawOffset()));
            // 5. 挂点 yaw / pitch / roll（与占位物品模型同一顺序）
            RiggingSocketTransforms.applyMountRotations(poseStack, pose);
            // 6. 模型缩放
            poseStack.scale(def.scale(), def.scale(), def.scale());

            renderer.render(poseStack, animatable, bufferSource, null, null, packedLight, partialTick);
            poseStack.popPose();
        }
    }

    /**
     * 预留开火动画入口。Phase 2 还没有开火事件调用；下一阶段从开火包/客户端事件里
     * 按「玩家 + 物品」找到对应 layer 再调这里。
     *
     * <p>不会为了触发动画而新建 animatable：这个玩家没渲染过本模型时直接忽略。
     */
    public void playFire(AbstractClientPlayer player) {
        RiggingAnimatable animatable = animatables.get(player);
        if (animatable != null) {
            animatable.playFire();
        }
    }

    private boolean hasMountOfThisModel(RiggingLoadout loadout) {
        for (RiggingMount mount : loadout.mounts()) {
            if (isThisModel(mount)) return true;
        }
        return false;
    }

    private boolean isThisModel(RiggingMount mount) {
        return def.equals(RiggingModelRegistry.find(mount.stack()));
    }
}
