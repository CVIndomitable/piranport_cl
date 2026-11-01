package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piranport.PiranPort;
import com.piranport.client.gecko.RiggingAnimatable;
import com.piranport.client.gecko.RiggingHullModelRegistry;
import com.piranport.client.gecko.RiggingModelDef;
import com.piranport.client.gecko.RiggingModelRegistry;
import com.piranport.client.gecko.RiggingObjectRenderer;
import com.piranport.client.gecko.RiggingRenderContext;
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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import software.bernie.geckolib.cache.GeckoLibCache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 第一人称舰装渲染。
 *
 * <p><b>为什么不用 RenderLayer</b>：第一人称不渲染玩家实体模型，整棵 RenderLayer 树不会执行；
 * 而第一人称手部（{@code RenderHandEvent}）的坐标系是手部骨骼，和「舰装挂在玩家身上的位置」
 * 对不上。这里直接在 {@link RenderLevelStageEvent} 的 PoseStack 里画。
 *
 * <p><b>PoseStack 的真实坐标系</b>：1.21.1 的 {@code RenderLevelStageEvent} 栈是
 * <b>相机相对、世界轴向</b>的——原点在相机位置，相机旋转在全局 model-view 矩阵里，
 * 不在 PoseStack 上（实体渲染也是 {@code translate(entity - camera)} 后直接画）。
 * 所以这里可以按世界坐标平移、只应用身体朝向；相机抬头/低头不会带动舰装，
 * 低头时舰装自然留在身体周围、出现在视野下方。
 *
 * <p><b>朝向换算</b>：舰装 geo 模型的船体/甲板为 +Y 朝上、<b>+Z 为船首</b>、+X 为左舷
 * （注意 {@code 小工具/rigging_geo_preview.py} 顶部写的「-Z 朝前」对船体不适用，
 * 实机确认船首在 +Z）。世界轴 +Y 朝上、+Z 朝南；玩家身体 yaw=0 朝南、yaw 增大转向西。
 * 因此绕世界 Y 转 {@code -bodyYaw} 后：模型 +Z（船首）指向身体前方、+X（左舷）在身体左侧，
 * 不需要第三人称那套 {@code Axis.XP(180)}（那是玩家模型帧 +Y 朝下专用的），也不需要镜像。
 *
 * <p><b>只画本地玩家</b>：第一人称视野里只有自己的舰装。
 *
 * <p><b>背部遮挡</b>：{@code general_hull} 的烟囱/桅杆（stack）从模型原点向上伸出近 1 格，
 * 默认会把视线中央糊住，所以默认只保留左右机械臂（跳过 hull_base / stack 的方块，
 * 但保留它们的子骨骼）。终端 {@code global.rigging_fp.show_body} 可开回完整船体做对比。
 *
 * <p><b>挂载武器</b>：船体画完后按 {@link RiggingSocketTransforms#mountPose} 逐个挂点补画。
 * 挂点坐标是第三人称玩家模型空间（+Y 朝下、+Z 朝前），换算到这里的身体模型空间
 * （+Y 朝上、+Z 船首）只需 {@code (x, -y, z)}；不再套第三人称的 {@code Axis.XP(180)}。
 * 三类武器各有自己的补偿角（见 {@link #renderGeckoWeapon} / {@link #renderItemWeapon}），
 * 原因写在方法注释里。
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

    /** 已登记 GeckoLib 的武器（目前是四种导弹发射器）按玩家 × 模型维护动画状态。 */
    private static final Map<AbstractClientPlayer, Map<RiggingModelDef, RiggingAnimatable>>
            MISSILE_ANIMATABLES = new WeakHashMap<>();
    /** 每个 GeckoLib 武器模型一个对象渲染器。 */
    private static final Map<RiggingModelDef, RiggingObjectRenderer> MISSILE_RENDERERS = new HashMap<>();

    private RiggingFirstPersonLayer() {}

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (!ModEquipmentConfig.RIGGING_FP_ENABLED.get()) return;

        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        // 旁观/第三人称（含前置视角）都不画：前置视角下相机不在眼睛位置，身体相对位姿会错位。
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

        // 身体朝向取插值后的 yBodyRot，不用头部/相机 pitch。
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        Vec3 camera = event.getCamera().getPosition();
        Vec3 eye = player.getEyePosition(partialTick);
        double offsetX = ModEquipmentConfig.RIGGING_FP_OFFSET_X.get();
        double offsetY = ModEquipmentConfig.RIGGING_FP_OFFSET_Y.get();
        double offsetZ = ModEquipmentConfig.RIGGING_FP_OFFSET_Z.get();

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        // 1. 从「相机为原点的世界轴栈」平移到玩家眼睛；offset_y 直接沿世界 Y 抬高/压低。
        poseStack.translate(eye.x - camera.x, eye.y - camera.y + offsetY, eye.z - camera.z);
        // 2. 只应用身体朝向：模型 +Z（船首）跟身体前方，头部俯仰不参与。
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        // 3. 身体系水平微调：+X 是左舷、+Z 是船首；终端语义保持「+offset_x 屏幕右、-offset_z 前方」。
        poseStack.translate(-offsetX, 0.0, -offsetZ);
        // 4. 终端微调角度，最后缩放。
        poseStack.mulPose(Axis.YP.rotationDegrees(
                ModEquipmentConfig.RIGGING_FP_YAW.get().floatValue()));
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

        // 挂载武器最后画：和船体共用同一个身体变换（含 scale / offset / yaw-pitch-roll）。
        renderWeapons(poseStack, bufferSource, packedLight, partialTick, player, loadout);
        // 物品渲染（FIXED）与 BEWLR 3D 模型的批次留到这时统一提交。
        bufferSource.endBatch();

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

    /** 逐个挂点补画武器：GeckoLib 导弹走模型渲染，其余走物品渲染管线。 */
    private static void renderWeapons(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                      int packedLight, float partialTick,
                                      AbstractClientPlayer player, RiggingLoadout loadout) {
        for (RiggingMount mount : loadout.mounts()) {
            // 第一人称不跟随 third-person 的放飞后仰；mountPose 目前也不使用该参数。
            RiggingSocketTransforms.MountPose pose =
                    RiggingSocketTransforms.mountPose(mount.socket(), 0.0f);
            RiggingModelDef def = RiggingModelRegistry.find(mount.stack());
            if (def != null) {
                renderGeckoWeapon(poseStack, bufferSource, packedLight, partialTick,
                        player, def, pose);
            } else {
                renderItemWeapon(poseStack, bufferSource, packedLight, player, mount, pose);
            }
        }
    }

    /**
     * 已登记 GeckoLib 的导弹发射器（{@code RiggingAttachedLayer} 的同一批模型）。
     *
     * <p>第三人称的顺序是 {@code XP(180) → YP(yawOffset) → 挂点 yaw/pitch/roll}；
     * 第一人称的模型空间本来就是 +Y 朝上，不需要 XP(180)，直接把
     * {@code yawOffset + 挂点 yaw} 合起来用。这样 sy1 这类 -Z 朝前的模型和 terrier
     * 这类 +Z 朝前的模型都会朝身体前方。
     */
    private static void renderGeckoWeapon(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                          int packedLight, float partialTick,
                                          AbstractClientPlayer player, RiggingModelDef def,
                                          RiggingSocketTransforms.MountPose pose) {
        poseStack.pushPose();
        // 挂点坐标是玩家模型空间（+Y 下），换到身体模型空间（+Y 上）只翻 y。
        poseStack.translate(pose.x() / 16.0f, -pose.y() / 16.0f, pose.z() / 16.0f);
        poseStack.mulPose(Axis.YP.rotationDegrees(def.yawOffset() + pose.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(pose.pitch()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pose.roll()));
        poseStack.scale(def.scale(), def.scale(), def.scale());
        renderPart(poseStack, bufferSource, packedLight, partialTick, def,
                missileAnimatable(player, def), missileRenderer(def));
        poseStack.popPose();
    }

    /**
     * 未登记 GeckoLib 的挂载武器：主炮/鱼雷/深弹/防空导弹走 BEWLR，飞机走平面图标。
     *
     * <p><b>为什么 3D 武器要额外 yaw+180</b>：这些 BEWLR 模型的父模型是
     * {@code minecraft:block/block}，它的 FIXED 显示变换只有 scale 0.5、<b>没有</b> Y180
     * （Y180 是 {@code item/generated} 的固定变换）。第三人称靠 {@code XP(180)} 把炮口+
     * 底座摆正；第一人称不能套 XP(180)，所以用挂点 yaw+180 达到同样的「炮口朝前、底座朝下」。
     *
     * <p><b>飞机为什么用另一组符号</b>：飞机是 {@code item/generated} 的平面图标，
     * FIXED 自带 Y180。抵消这 Y180 后，等效角变成 {@code yaw / -pitch / -roll}；
     * DECK 的 pitch=-90 因此变成 +90，图标平铺、正面朝上，从上方能看到正确的图标。
     */
    private static void renderItemWeapon(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                         int packedLight, AbstractClientPlayer player,
                                         RiggingMount mount, RiggingSocketTransforms.MountPose pose) {
        poseStack.pushPose();
        poseStack.translate(pose.x() / 16.0f, -pose.y() / 16.0f, pose.z() / 16.0f);

        boolean flatIcon = isDeckSocket(mount.socket());
        if (flatIcon) {
            poseStack.mulPose(Axis.YP.rotationDegrees(pose.yaw()));
            poseStack.mulPose(Axis.XP.rotationDegrees(-pose.pitch()));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-pose.roll()));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(pose.yaw() + 180.0f));
            poseStack.mulPose(Axis.XP.rotationDegrees(pose.pitch()));
            poseStack.mulPose(Axis.ZP.rotationDegrees(pose.roll()));
        }
        poseStack.scale(pose.scale(), pose.scale(), pose.scale());

        // BEWLR 无法区分「展示框的 FIXED」和「舰装挂点的 FIXED」，用标记告诉它这次要 3D。
        RiggingRenderContext.setRiggingPass(true);
        try {
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    mount.stack(), ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                    poseStack, bufferSource, player.level(),
                    player.getId() * 31 + mount.socket().ordinal());
        } finally {
            RiggingRenderContext.setRiggingPass(false);
        }
        poseStack.popPose();
    }

    /** 甲板挂点只放飞机（平面图标）；其它挂点都是 3D 武器模型或 GeckoLib 导弹。 */
    private static boolean isDeckSocket(RiggingSocket socket) {
        return socket == RiggingSocket.DECK_1 || socket == RiggingSocket.DECK_2;
    }

    private static RiggingAnimatable missileAnimatable(AbstractClientPlayer player, RiggingModelDef def) {
        return MISSILE_ANIMATABLES
                .computeIfAbsent(player, ignored -> new HashMap<>())
                .computeIfAbsent(def, RiggingAnimatable::new);
    }

    private static RiggingObjectRenderer missileRenderer(RiggingModelDef def) {
        return MISSILE_RENDERERS.computeIfAbsent(def, RiggingObjectRenderer::new);
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
