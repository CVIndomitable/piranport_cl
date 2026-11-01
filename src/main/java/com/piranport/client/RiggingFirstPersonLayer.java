package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piranport.PiranPort;
import com.piranport.client.gecko.RiggingAnimatable;
import com.piranport.client.gecko.RiggingHullModelRegistry;
import com.piranport.client.gecko.RiggingObjectRenderer;
import com.piranport.client.gecko.RiggingRenderDef;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.rigging.RiggingCategory;
import com.piranport.rigging.RiggingLoadout;
import com.piranport.rigging.RiggingMount;
import com.piranport.rigging.RiggingSocket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import software.bernie.geckolib.cache.GeckoLibCache;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 第一人称舰装渲染。
 *
 * <p><b>为什么不用 RenderLayer</b>：第一人称不渲染玩家实体模型，整棵 RenderLayer 树不会执行；
 * 而第一人称手部（{@code RenderHandEvent}）的坐标系是手部骨骼，和「舰装挂在玩家身上的位置」
 * 对不上。这里直接在 {@link RenderLevelStageEvent} 的相机相对世界栈里画，天然就是视角相对的：
 * 相机旋转已经烘焙进 poseStack，平移/旋转都以「屏幕前、屏幕两侧」为基准，转头时舰装跟着视角走。
 *
 * <p><b>坐标系换算</b>：舰装 geo 模型的船体/甲板为 +Y 朝上、<b>+Z 为船首</b>、+X 为左舷
 * （注意 {@code 小工具/rigging_geo_preview.py} 顶部写的「-Z 朝前」对船体不适用，
 * 实机确认船首在 +Z）。相机相对栈为 +Y 朝上、-Z 为视线前方、+X 为屏幕右。
 * 水平转 180° 后：模型 +Z（船首）→ view -Z（视线前方），模型 +X（左）→ view -X（屏幕左），
 * +Y 保持朝上，正好是「站在舰装内部」的视角。第三人称那套 {@code Axis.XP(180)} 是
 * 玩家模型帧（+Y 朝下）专用的，这里不能照搬。
 *
 * <p><b>只画本地玩家</b>：第一人称视野里只有自己的舰装。
 *
 * <p><b>背部遮挡</b>：{@code general_hull} 的烟囱/桅杆（stack）从模型原点向上伸出近 1 格，
 * 默认会把视线中央糊住，所以默认只保留左右机械臂（跳过 hull_base / stack 的方块，
 * 但保留它们的子骨骼）。终端 {@code global.rigging_fp.show_body} 可开回完整船体做对比。
 *
 * <p>平台上的武器模型（{@code RiggingAttachedLayer} 那一套）本阶段不参与第一人称，
 * 等下一轮把挂点接到这里的船体位姿后再加。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public final class RiggingFirstPersonLayer {

    /** 默认隐藏「躯干 + 烟囱/桅杆」自身的方块；arm_left / arm_right 是 hull_base 的子骨骼，仍会渲染。 */
    private static final Set<String> BODY_BONES = Set.of("hull_base", "stack");

    private static final Map<AbstractClientPlayer, FpAnimatables> ANIMATABLES = new WeakHashMap<>();

    /** 已警告过的缺失模型，避免每帧刷日志。只在渲染线程读写。 */
    private static final Set<ResourceLocation> REPORTED_MISSING = new HashSet<>();

    private static final RiggingObjectRenderer HULL_RENDERER =
            new RiggingObjectRenderer(RiggingHullModelRegistry.GENERAL_HULL);
    private static final RiggingObjectRenderer DECK_RENDERER =
            new RiggingObjectRenderer(RiggingHullModelRegistry.CARRIER_DECK);
    private static final RiggingObjectRenderer WAIST_RACK_RENDERER =
            new RiggingObjectRenderer(RiggingHullModelRegistry.WAIST_TORPEDO_RACK);

    private RiggingFirstPersonLayer() {}

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (!ModEquipmentConfig.RIGGING_FP_ENABLED.get()) return;

        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        // 旁观/第三人称（含前置视角）都不画：前置视角下相机会跑到身体前方，这套「视角相对」位姿会错位。
        if (!mc.options.getCameraType().isFirstPerson()) return;
        if (mc.getCameraEntity() != player) return;
        if (player.isInvisible()) return;
        if (!RiggingClientState.isRenderedAsTransformed(player)) return;

        RiggingLoadout loadout = RiggingClientState.resolveLoadout(player);
        // EMPTY 是「未变身 / 还没快照」的哨兵；已变身但快捷栏没武器仍然要画空舰装。
        if (loadout.isEmpty()) return;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        int packedLight = LevelRenderer.getLightColor(mc.level,
                BlockPos.containing(player.getEyePosition(partialTick)));

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        // 视角相对：先在相机前方摆好位置，再绕视线做 yaw/pitch/roll，最后缩放。
        poseStack.translate(
                ModEquipmentConfig.RIGGING_FP_OFFSET_X.get(),
                ModEquipmentConfig.RIGGING_FP_OFFSET_Y.get(),
                ModEquipmentConfig.RIGGING_FP_OFFSET_Z.get());
        // 基础 180°：模型 +Z 为船首、相机前方是 -Z；转过来后船首朝视线前方、左舷落在屏幕左侧。
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f
                + ModEquipmentConfig.RIGGING_FP_YAW.get().floatValue()));
        poseStack.mulPose(Axis.XP.rotationDegrees(
                ModEquipmentConfig.RIGGING_FP_PITCH.get().floatValue()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(
                ModEquipmentConfig.RIGGING_FP_ROLL.get().floatValue()));
        float scale = ModEquipmentConfig.RIGGING_FP_SCALE.get().floatValue();
        poseStack.scale(scale, scale, scale);

        FpAnimatables animatables = animatablesFor(player);
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        HULL_RENDERER.setHiddenBoneCubes(ModEquipmentConfig.RIGGING_FP_SHOW_BODY.get()
                ? Set.of() : BODY_BONES);
        renderPart(poseStack, bufferSource, packedLight, partialTick,
                RiggingHullModelRegistry.GENERAL_HULL, animatables.generalHull(), HULL_RENDERER);

        if (loadout.hasAircraft()) {
            renderPart(poseStack, bufferSource, packedLight, partialTick,
                    RiggingHullModelRegistry.CARRIER_DECK, animatables.carrierDeck(), DECK_RENDERER);
        }

        if (hasWaistTorpedoMount(loadout)) {
            renderPart(poseStack, bufferSource, packedLight, partialTick,
                    RiggingHullModelRegistry.WAIST_TORPEDO_RACK, animatables.waistRack(),
                    WAIST_RACK_RENDERER);
        }

        poseStack.popPose();
    }

    /**
     * 画一个船体部件并立刻提交它的缓冲区。
     *
     * <p>{@code GeoObjectRenderer} 内部自己去拿 {@code Minecraft} 的 BufferSource，
     * 这里只用同一份 renderType 把批处理提交掉，避免几何留在缓冲区里被后续渲染覆盖。
     */
    private static void renderPart(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                   int packedLight, float partialTick,
                                   RiggingRenderDef def,
                                   RiggingAnimatable animatable, RiggingObjectRenderer renderer) {
        // 资产缺失时跳过而不是崩客户端；只警告一次，避免每帧刷日志。
        if (!GeckoLibCache.getBakedModels().containsKey(def.geoModel())) {
            if (REPORTED_MISSING.add(def.geoModel())) {
                PiranPort.LOGGER.warn("第一人称舰装模型未加载，已跳过渲染: {}", def.geoModel());
            }
            return;
        }
        renderer.render(poseStack, animatable, bufferSource, null, null, packedLight, partialTick);
        bufferSource.endBatch(RenderType.entityCutoutNoCull(def.texture()));
    }

    private static FpAnimatables animatablesFor(AbstractClientPlayer player) {
        return ANIMATABLES.computeIfAbsent(player, ignored -> new FpAnimatables(
                new RiggingAnimatable(RiggingHullModelRegistry.GENERAL_HULL),
                new RiggingAnimatable(RiggingHullModelRegistry.CARRIER_DECK),
                new RiggingAnimatable(RiggingHullModelRegistry.WAIST_TORPEDO_RACK)));
    }

    /** 与第三人称 {@code RiggingHullLayer} 同一判据：socket 是 WAIST_*，或类别是 TORPEDO。 */
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

    /** 每个玩家 × 每个船体部件一份动画状态，避免动画串台（目前只画本地玩家，保留结构以防扩展）。 */
    private record FpAnimatables(
            RiggingAnimatable generalHull,
            RiggingAnimatable carrierDeck,
            RiggingAnimatable waistRack) {}
}
