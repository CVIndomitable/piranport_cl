package com.piranport.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.piranport.PiranPort;
import com.piranport.artillery.ArtilleryItem;
import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.aviation.ClientFireControlData;
import com.piranport.combat.BallisticSolver;
import com.piranport.combat.CombatTargeting;
import com.piranport.combat.FireControlPrediction;
import com.piranport.combat.TransformationManager;
import com.piranport.combat.util.CombatFireUtils;
import com.piranport.component.LoadedAmmo;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.item.TorpedoItem;
import com.piranport.item.TorpedoLauncherItem;
import com.piranport.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.List;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * 火控可视化预测（策划决策/火控/06）。纯客户端：不改火控列表，不向服务端发坐标。
 *
 * <ul>
 *   <li>手持火炮：第一个火控锁定目标前方的预测落点画成 <b>HUD 标记</b>（固定像素大小），
 *       准星射线进入落点球范围（世界空间判定，半径 1 格）时标记变绿。</li>
 *   <li>手持鱼雷发射器：世界里从玩家画白线到水平拦截点，多联装再画以该线为轴的扇形，
 *       圆心角 = 2 × max|散布角|；拦截点本身同样画成 HUD 标记。
 *       玩家水平朝向落在扇形（单管 ±1°）内时标记变绿。</li>
 * </ul>
 * 目标速度按 5 tick 位置采样窗口计算；每个客户端 tick 重算，渲染阶段只绘制最近结果并插值。
 *
 * <p><b>为什么预瞄点画在 HUD 而不是世界里</b>：世界空间的球按透视缩小，越远越小越快看不清
 * （远距离狙击恰恰是最需要落点提示的场景）。改成把落点投影到屏幕、以固定像素尺寸绘制后，
 * 屏幕距离仍能传达"偏左/偏右"，而"看得清"不再随距离劣化。鱼雷的线/扇形是 3D 跨度，
 * 投影到屏幕等于退化成一条线，所以那部分保留在世界空间。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public final class FireControlVisualRenderer {

    private static final double MAX_RENDER_DISTANCE = 256.0;
    private static final int PREDICTION_ITERATIONS = 2;
    private static final double GRAVITY_SCALE = 196.0;
    private static final double MIN_SPEED = 1.0e-3;
    /** 落点球直径 1.0 格（文档 §4）。 */
    static final double SPHERE_RADIUS = 0.5;
    private static final double SINGLE_TUBE_ACTIVE_DEG = 1.0;
    /** HUD 预瞄标记半径（GUI 缩放像素）与贴边留白。 */
    private static final int HUD_RADIUS = 7;
    private static final int HUD_MARGIN = 12;
    /** 圆圈环宽（像素）：1 = 只有最外一圈像素，中心完全留空不遮准星。 */
    private static final int HUD_RING_THICKNESS = 1;
    private static final int ALPHA = 150;

    private static final RenderType LINES = RenderType.create(
            "piranport_fire_control_visual",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.DEBUG_LINES,
            1024,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2.0)))
                    .createCompositeState(false));

    private enum Mode { NONE, CANNON, TORPEDO }

    private static final EntityUuidCache ENTITY_CACHE = new EntityUuidCache();
    private static final FireControlPrediction.PositionSampler SAMPLER = new FireControlPrediction.PositionSampler();
    private static UUID sampledTarget;

    private static Mode mode = Mode.NONE;
    private static boolean active;
    /** CANNON: 球心；TORPEDO: 拦截点。 */
    private static Vec3 point;
    private static Vec3 previousPoint;
    /** TORPEDO: 线起点（玩家发射位置）。 */
    private static Vec3 origin;
    private static Vec3 previousOrigin;
    private static double fanHalfDeg;
    private static Vec3 renderCamera = Vec3.ZERO;

    // ===== HUD 投影结果（世界渲染阶段算好，GUI 阶段直接画）=====
    /** 本帧是否有可画的预瞄标记（相机背后 / 超出渲染距离时为 false）。 */
    private static boolean hudVisible;
    /** 标记是否落在屏幕内；false 表示已贴边钳制（目标在视野外）。 */
    private static boolean hudOnScreen;
    private static float hudX, hudY;

    private FireControlVisualRenderer() {}

    /** 每个客户端 tick 在火控输入处理之后调用。 */
    public static void tick(Minecraft mc) {
        Mode previousMode = mode;
        previousPoint = point;
        previousOrigin = origin;
        mode = Mode.NONE;
        point = null;
        origin = null;
        active = false;
        fanHalfDeg = 0;
        update(mc);
        if (mode != previousMode) {
            previousPoint = null;
            previousOrigin = null;
        }
    }

    public static void reset() {
        mode = Mode.NONE;
        point = previousPoint = origin = previousOrigin = null;
        active = false;
        hudVisible = false;
        hudOnScreen = false;
        SAMPLER.clear();
        sampledTarget = null;
        ENTITY_CACHE.clear();
    }

    private static void update(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;
        List<UUID> targets = ClientFireControlData.getTargets();
        if (targets.isEmpty()) {
            SAMPLER.clear();
            sampledTarget = null;
            return;
        }
        UUID targetId = targets.get(0);
        Entity target = findTarget(mc, targetId);
        if (target == null || !target.isAlive()) {
            SAMPLER.clear();
            sampledTarget = null;
            return;
        }
        if (!targetId.equals(sampledTarget)) {
            SAMPLER.clear();
            sampledTarget = targetId;
        }
        SAMPLER.push(target.position());
        Vec3 targetVelocity = SAMPLER.velocity();

        ItemStack held = mc.player.getMainHandItem();
        boolean transformed = TransformationManager.isPlayerTransformed(mc.player);
        boolean creative = mc.player.getAbilities().instabuild;
        if (held.getItem() instanceof ArtilleryItem artillery && (transformed || creative)) {
            updateCannon(mc, artillery, target, targetVelocity);
        } else if (held.getItem() instanceof TorpedoLauncherItem launcher && (transformed || creative)) {
            updateTorpedo(mc, held, launcher, target, targetVelocity);
        }
    }

    private static void updateCannon(Minecraft mc, ArtilleryItem artillery, Entity target, Vec3 targetVelocity) {
        ArtilleryCannonData data = artillery.getEffectiveData(mc.level);
        double speed = data.initialSpeed();
        if (!Double.isFinite(speed) || speed <= MIN_SPEED) return;
        double drag = Math.max(0.0, data.dragCoeff());
        double gravity = data.gravity() > 0.0f ? data.gravity() / GRAVITY_SCALE : BallisticSolver.DEFAULT_GRAVITY;
        Vec3 eye = mc.player.getEyePosition();
        // 统一瞄点：取目标眼睛位置，与实弹瞄准口径一致（见 CombatTargeting#aimPoint）。
        Vec3 aim = CombatTargeting.aimPoint(target);
        Vec3 predicted = BallisticSolver.predictImpactPoint(eye, aim, targetVelocity, speed, drag, gravity,
                BallisticSolver.UNRESTRICTED_MIN_ANGLE, Math.toRadians(data.maxElevation()), PREDICTION_ITERATIONS);
        if (!isFinite(predicted) || predicted.distanceToSqr(eye) > MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE) return;
        mode = Mode.CANNON;
        point = predicted;
        active = FireControlPrediction.rayHitsSphere(eye, mc.player.getViewVector(1.0f), predicted, SPHERE_RADIUS);
    }

    private static void updateTorpedo(Minecraft mc, ItemStack held, TorpedoLauncherItem launcher,
                                      Entity target, Vec3 targetVelocity) {
        double speed = torpedoSpeed(held, launcher);
        Vec3 start = new Vec3(mc.player.getX(), mc.player.getEyeY() - 0.3, mc.player.getZ());
        Vec3 hit = FireControlPrediction.torpedoIntercept(start, target.position(), targetVelocity, speed);
        if (hit == null) return;
        hit = new Vec3(hit.x, start.y, hit.z);
        if (hit.distanceToSqr(start) > MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE) return;
        mode = Mode.TORPEDO;
        origin = start;
        point = hit;
        int tubes = launcher.getTubeCount();
        fanHalfDeg = tubes >= 2
                ? FireControlPrediction.fanAngleDegrees(CombatFireUtils.getSpreadAngles(tubes)) / 2.0 : 0.0;
        // 激活：玩家水平朝向落在扇形内（单管取 ±1°）。
        Vec3 look = mc.player.getLookAngle();
        Vec3 toHit = hit.subtract(start);
        double lookYaw = Math.atan2(look.z, look.x);
        double hitYaw = Math.atan2(toHit.z, toHit.x);
        double diff = Math.toDegrees(Math.abs(Math.atan2(Math.sin(lookYaw - hitYaw), Math.cos(lookYaw - hitYaw))));
        active = diff <= Math.max(SINGLE_TUBE_ACTIVE_DEG, fanHalfDeg);
    }

    /** 与 TorpedoFireStrategy 一致：装填弹种读 TorpedoItem.getSpeed()，否则 610mm 1.0 / 其他 1.2。 */
    private static double torpedoSpeed(ItemStack launcherStack, TorpedoLauncherItem launcher) {
        LoadedAmmo loaded = launcherStack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (loaded.hasAmmo()) {
            ResourceLocation id = ResourceLocation.tryParse(loaded.ammoItemId());
            Item item = id == null ? null : BuiltInRegistries.ITEM.get(id);
            if (item instanceof TorpedoItem torpedo) return torpedo.getSpeed();
        }
        return launcher.getCaliber() == 610 ? 1.0 : 1.2;
    }

    private static Entity findTarget(Minecraft mc, UUID uuid) {
        Entity cached = ENTITY_CACHE.get(mc.level, uuid);
        return cached != null ? cached : mc.level.getPlayerByUUID(uuid);
    }

    private static boolean isFinite(Vec3 v) {
        return v != null && Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

    // ===== 渲染 =====

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        // 先清本帧的 HUD 状态：下面任何一条提前返回都不能留下上一帧的标记
        hudVisible = false;
        if (mode == Mode.NONE || point == null) return;
        Minecraft mc = Minecraft.getInstance();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        Vec3 renderPoint = previousPoint == null ? point : previousPoint.lerp(point, partial);
        if (renderPoint.distanceToSqr(camera) > MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE) return;

        int rgb = active ? ModEquipmentConfig.PREDICTION_LINE_ACTIVE_COLOR.get()
                : ModEquipmentConfig.PREDICTION_LINE_COLOR.get();
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;

        projectToHud(event, renderPoint, mc);

        // 预瞄点本身画在 HUD（见类注释）；世界空间只保留鱼雷的线/扇形这类 3D 跨度提示。
        if (mode == Mode.TORPEDO) {
            Vec3 start = previousOrigin == null || origin == null ? origin : previousOrigin.lerp(origin, partial);
            if (start != null && fanHalfDeg > 0) {
                PoseStack poseStack = event.getPoseStack();
                poseStack.pushPose();
                renderCamera = camera;
                Matrix4f pose = poseStack.last().pose();
                VertexConsumer c = mc.renderBuffers().bufferSource().getBuffer(LINES);
                line(c, pose, start, renderPoint, r, g, b);
                drawFan(c, pose, start, renderPoint, fanHalfDeg, r, g, b);
                mc.renderBuffers().bufferSource().endBatch(LINES);
                poseStack.popPose();
            }
        }
    }

    /**
     * 把预测落点投影到屏幕（GUI 缩放坐标）。
     *
     * <p>用本阶段真实的投影矩阵与相机朝向，因此开镜缩放（改的是 FOV，即投影矩阵）自动跟随，
     * 不需要另算一份 FOV。WHY 在这里算而不是在 GUI 阶段算：GUI 阶段 RenderSystem 的投影矩阵
     * 已被换成正交矩阵，那时再投影必然错。
     */
    private static void projectToHud(RenderLevelStageEvent event, Vec3 worldPoint, Minecraft mc) {
        hudVisible = false;
        FireControlPrediction.ScreenPoint p = FireControlPrediction.projectToScreen(
                worldPoint, event.getCamera().getPosition(), event.getCamera().rotation(),
                event.getProjectionMatrix(),
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), HUD_MARGIN);
        if (p == null) return;
        hudX = p.x();
        hudY = p.y();
        hudOnScreen = p.onScreen();
        hudVisible = true;
    }

    /** HUD 层：每帧把预测落点画成固定像素大小的圆圈标记。 */
    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.CROSSHAIR) || !hudVisible) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int rgb = active ? ModEquipmentConfig.PREDICTION_LINE_ACTIVE_COLOR.get()
                : ModEquipmentConfig.PREDICTION_LINE_COLOR.get();
        drawHudMarker(event.getGuiGraphics(), Math.round(hudX), Math.round(hudY), rgb, hudOnScreen);
    }

    /**
     * 预瞄标记：屏幕内画圆圈（圆环，中心留空不遮准星），视野外画实心小方块
     * （说明"落点在那个方向、不在视野里"）。固定像素尺寸 —— 这正是从世界空间搬到 HUD 的目的。
     */
    private static void drawHudMarker(GuiGraphics g, int x, int y, int rgb, boolean onScreen) {
        int color = 0xFF000000 | rgb;
        if (!onScreen) {
            g.fill(x - 3, y - 3, x + 3, y + 3, 0xFF000000 | rgb);
            return;
        }
        // 圆环 = 半径 HUD_RADIUS 的实心圆 − 半径小 HUD_RING_THICKNESS 的内圆。
        // 逐行扫描线求两个圆在该行的半宽，差集即左右两段环；内圆半宽钳到「外圆半宽 − 1」
        // 是为了在圆顶/圆底取整后两半宽相等时仍留出 1 像素环，避免圆出现缺口。
        for (int dy = -HUD_RADIUS; dy <= HUD_RADIUS; dy++) {
            int outer = rowHalfWidth(HUD_RADIUS, dy);
            int inner = Math.min(rowHalfWidth(HUD_RADIUS - HUD_RING_THICKNESS, dy), outer - 1);
            g.fill(x - outer, y + dy, x - inner, y + dy + 1, color);      // 左半环
            g.fill(x + inner + 1, y + dy, x + outer + 1, y + dy + 1, color); // 右半环
        }
    }

    /** 实心圆在第 dy 行（圆心所在行为 0）的半宽像素数；该行整行落在圆外时返回 -1。 */
    private static int rowHalfWidth(int radius, int dy) {
        if (Math.abs(dy) > radius) return -1;
        return (int) Math.round(Math.sqrt((double) radius * radius - (double) dy * dy));
    }

    /** 以 start→end 为对称轴的水平扇形：两条边线 + 圆弧。 */
    private static void drawFan(VertexConsumer c, Matrix4f pose, Vec3 start, Vec3 end, double halfDeg,
                                int r, int g, int b) {
        Vec3 axis = end.subtract(start);
        double len = Math.sqrt(axis.x * axis.x + axis.z * axis.z);
        if (len < 1.0e-3) return;
        double baseYaw = Math.atan2(axis.z, axis.x);
        double half = Math.toRadians(halfDeg);
        int arcSegments = Math.max(4, (int) Math.ceil(halfDeg * 2));
        Vec3 prev = null;
        for (int i = 0; i <= arcSegments; i++) {
            double yaw = baseYaw - half + (2 * half) * i / arcSegments;
            Vec3 p = start.add(Math.cos(yaw) * len, 0, Math.sin(yaw) * len);
            if (i == 0 || i == arcSegments) line(c, pose, start, p, r, g, b);
            if (prev != null) line(c, pose, prev, p, r, g, b);
            prev = p;
        }
    }

    /** 先在 double 精度下减去相机位置再转 float，避免远离原点时顶点抖动。 */
    private static void line(VertexConsumer c, Matrix4f pose, Vec3 a, Vec3 b, int r, int g, int bl) {
        Vec3 cam = renderCamera;
        c.addVertex(pose, (float) (a.x - cam.x), (float) (a.y - cam.y), (float) (a.z - cam.z)).setColor(r, g, bl, ALPHA);
        c.addVertex(pose, (float) (b.x - cam.x), (float) (b.y - cam.y), (float) (b.z - cam.z)).setColor(r, g, bl, ALPHA);
    }
}
