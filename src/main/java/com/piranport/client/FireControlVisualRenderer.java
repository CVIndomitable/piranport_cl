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
import com.piranport.combat.FireControlPrediction;
import com.piranport.combat.TransformationManager;
import com.piranport.combat.util.CombatFireUtils;
import com.piranport.component.LoadedAmmo;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.item.TorpedoItem;
import com.piranport.item.TorpedoLauncherItem;
import com.piranport.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
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
 *   <li>手持火炮：第一个火控锁定目标前方渲染直径 1 格的半透明白球，准星射线进入球内变绿。</li>
 *   <li>手持鱼雷发射器：从玩家画白线到水平拦截点，末端小球；多联装画以该线为轴的扇形，
 *       圆心角 = 2 × max|散布角|。玩家水平朝向落在扇形（单管 ±1°）内时变绿。</li>
 * </ul>
 * 目标速度按 5 tick 位置采样窗口计算；每个客户端 tick 重算，渲染阶段只绘制最近结果并插值。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public final class FireControlVisualRenderer {

    private static final double MAX_RENDER_DISTANCE = 256.0;
    private static final int PREDICTION_ITERATIONS = 2;
    private static final double GRAVITY_SCALE = 196.0;
    private static final double MIN_SPEED = 1.0e-3;
    /** 落点球直径 1.0 格（文档 §4）。 */
    static final double SPHERE_RADIUS = 0.5;
    private static final double TORPEDO_END_RADIUS = 0.25;
    private static final double SINGLE_TUBE_ACTIVE_DEG = 1.0;
    private static final int SPHERE_SEGMENTS = 16;
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
        Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
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
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || mode == Mode.NONE || point == null) return;
        Minecraft mc = Minecraft.getInstance();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        Vec3 renderPoint = previousPoint == null ? point : previousPoint.lerp(point, partial);
        if (renderPoint.distanceToSqr(camera) > MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE) return;

        int rgb = active ? ModEquipmentConfig.PREDICTION_LINE_ACTIVE_COLOR.get()
                : ModEquipmentConfig.PREDICTION_LINE_COLOR.get();
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        renderCamera = camera;
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer c = mc.renderBuffers().bufferSource().getBuffer(LINES);

        if (mode == Mode.CANNON) {
            drawSphere(c, pose, renderPoint, SPHERE_RADIUS, r, g, b);
        } else {
            Vec3 start = previousOrigin == null || origin == null ? origin : previousOrigin.lerp(origin, partial);
            if (start != null) {
                line(c, pose, start, renderPoint, r, g, b);
                drawSphere(c, pose, renderPoint, TORPEDO_END_RADIUS, r, g, b);
                if (fanHalfDeg > 0) drawFan(c, pose, start, renderPoint, fanHalfDeg, r, g, b);
            }
        }
        mc.renderBuffers().bufferSource().endBatch(LINES);
        poseStack.popPose();
    }

    /** 线框球：3 个正交大圆 + 2 条纬线。 */
    private static void drawSphere(VertexConsumer c, Matrix4f pose, Vec3 center, double radius, int r, int g, int b) {
        for (int i = 0; i < SPHERE_SEGMENTS; i++) {
            double a0 = i * Math.PI * 2.0 / SPHERE_SEGMENTS;
            double a1 = (i + 1) * Math.PI * 2.0 / SPHERE_SEGMENTS;
            double c0 = Math.cos(a0) * radius, s0 = Math.sin(a0) * radius;
            double c1 = Math.cos(a1) * radius, s1 = Math.sin(a1) * radius;
            line(c, pose, center.add(c0, 0, s0), center.add(c1, 0, s1), r, g, b);
            line(c, pose, center.add(c0, s0, 0), center.add(c1, s1, 0), r, g, b);
            line(c, pose, center.add(0, s0, c0), center.add(0, s1, c1), r, g, b);
            double lat = radius * 0.5, ring = radius * Math.sqrt(0.75);
            line(c, pose, center.add(Math.cos(a0) * ring, lat, Math.sin(a0) * ring),
                    center.add(Math.cos(a1) * ring, lat, Math.sin(a1) * ring), r, g, b);
            line(c, pose, center.add(Math.cos(a0) * ring, -lat, Math.sin(a0) * ring),
                    center.add(Math.cos(a1) * ring, -lat, Math.sin(a1) * ring), r, g, b);
        }
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
