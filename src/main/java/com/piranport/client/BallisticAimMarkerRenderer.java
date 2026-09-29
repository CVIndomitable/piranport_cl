package com.piranport.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.piranport.PiranPort;
import com.piranport.aviation.ClientFireControlData;
import com.piranport.artillery.ArtilleryItem;
import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.combat.BallisticSolver;
import com.piranport.combat.TransformationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * 火控第一目标的炮弹落点预瞄圈。
 *
 * <p>这是纯客户端提示：它不修改火控列表，也不向服务端发送预测坐标。每个客户端 tick
 * 根据当前火炮的弹道参数和锁定目标速度重算，世界渲染阶段只绘制最近一次结果。</p>
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public final class BallisticAimMarkerRenderer {

    private static final double MAX_MARKER_DISTANCE = 256.0;
    // Two correction passes track normal ship motion while keeping the client tick inexpensive.
    private static final int PREDICTION_ITERATIONS = 2;
    private static final int MAX_FLIGHT_TICKS = 400;
    private static final double MIN_SPEED = 1.0e-3;
    private static final double GRAVITY_SCALE = 196.0;
    private static final int RING_SEGMENTS = 32;
    private static final int RING_RED = 255;
    private static final int RING_GREEN = 96;
    private static final int RING_BLUE = 32;
    private static final RenderType AIM_MARKER_RENDER_TYPE = RenderType.create(
            "piranport_ballistic_aim_marker",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.DEBUG_LINES,
            256,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2.5)))
                    .createCompositeState(false));

    private static Vec3 markerPosition;
    private static Vec3 previousMarkerPosition;
    private static double markerRadius;
    private static double previousMarkerRadius;
    private static final EntityUuidCache ENTITY_CACHE = new EntityUuidCache();

    private BallisticAimMarkerRenderer() {}

    /** Recalculate once per client tick, after the fire-control input has been processed. */
    public static void tick(Minecraft mc) {
        update(mc);
    }

    public static void reset() {
        markerPosition = null;
        previousMarkerPosition = null;
        markerRadius = 0.0;
        previousMarkerRadius = 0.0;
        ENTITY_CACHE.clear();
    }

    private static void update(Minecraft mc) {
        previousMarkerPosition = markerPosition;
        previousMarkerRadius = markerRadius;
        markerPosition = null;
        markerRadius = 0.0;
        if (mc.player == null || mc.level == null
                || (!TransformationManager.isPlayerTransformed(mc.player)
                && !(mc.player.getAbilities().instabuild
                && mc.player.getMainHandItem().getItem() instanceof ArtilleryItem))) return;

        List<UUID> targets = ClientFireControlData.getTargets();
        if (targets.isEmpty()) return;

        Entity target = findTarget(mc, targets.get(0));
        ItemStack held = mc.player.getMainHandItem();
        if (target == null || !target.isAlive() || !(held.getItem() instanceof ArtilleryItem artillery)) return;

        ArtilleryCannonData data = artillery.getEffectiveData(mc.level);
        double speed = data.initialSpeed();
        if (!Double.isFinite(speed) || speed <= MIN_SPEED) return;

        double drag = Math.max(0.0, data.dragCoeff());
        double gravity = data.gravity() > 0.0f
                ? data.gravity() / GRAVITY_SCALE
                : BallisticSolver.DEFAULT_GRAVITY;
        double minAngle = BallisticSolver.UNRESTRICTED_MIN_ANGLE;
        double maxAngle = Math.toRadians(data.maxElevation());

        Vec3 origin = mc.player.getEyePosition();
        Vec3 targetAimPoint = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
        Vec3 targetVelocity = target.getDeltaMovement();
        if (!isFinite(targetVelocity)) targetVelocity = Vec3.ZERO;
        // A malformed or desynchronized client velocity should never throw the marker across the map.
        if (targetVelocity.lengthSqr() > 16.0) targetVelocity = targetVelocity.normalize().scale(4.0);

        double flightTicks = estimateFlightTicks(origin, targetAimPoint, speed, drag, gravity,
                minAngle, maxAngle);
        Vec3 predicted = targetAimPoint;
        for (int i = 0; i < PREDICTION_ITERATIONS; i++) {
            predicted = targetAimPoint.add(targetVelocity.scale(flightTicks));
            flightTicks = estimateFlightTicks(origin, predicted, speed, drag, gravity,
                    minAngle, maxAngle);
        }

        Vec3 marker = predicted.add(0.0, -target.getBbHeight() * 0.45 + 0.08, 0.0);
        // Keep the ring visible above water when the target's aim point is submerged.
        if (target.isInWaterOrBubble()) {
            var surfacePos = target.blockPosition();
            int scan = 0;
            while (scan < 16 && !mc.level.getFluidState(surfacePos.above()).isEmpty()) {
                surfacePos = surfacePos.above();
                scan++;
            }
            double surface = surfacePos.getY() + 1.0;
            marker = new Vec3(marker.x, Math.max(marker.y, surface + 0.04), marker.z);
        }
        if (!isFinite(marker) || marker.distanceToSqr(origin) > MAX_MARKER_DISTANCE * MAX_MARKER_DISTANCE) return;
        markerPosition = marker;
        markerRadius = Math.max(0.75, Math.min(2.5, target.getBbWidth() * 0.8));
    }

    private static Entity findTarget(Minecraft mc, UUID uuid) {
        Entity cached = ENTITY_CACHE.get(mc.level, uuid);
        return cached != null ? cached : mc.level.getPlayerByUUID(uuid);
    }

    /** Estimate the same horizontal crossing time used by the projectile's drag/gravity order. */
    private static double estimateFlightTicks(Vec3 origin, Vec3 target, double speed, double drag,
                                              double gravity, double minAngle, double maxAngle) {
        Vec3 delta = target.subtract(origin);
        double horizontalDistance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontalDistance < 0.05) return 1.0;

        double angle = BallisticSolver.solve(speed, drag, gravity, horizontalDistance, delta.y,
                0.0, minAngle, maxAngle).angle();
        double horizontalVelocity = Math.max(0.01, speed * Math.cos(angle));
        double travelled = 0.0;
        double vanillaDrag = 0.99;
        double customDrag = 1.0 / (1.0 + Math.max(0.0, drag));
        for (int tick = 1; tick <= MAX_FLIGHT_TICKS; tick++) {
            horizontalVelocity *= customDrag;
            travelled += horizontalVelocity;
            if (travelled >= horizontalDistance) return tick;
            horizontalVelocity *= vanillaDrag;
        }
        return MAX_FLIGHT_TICKS;
    }

    private static boolean isFinite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || markerPosition == null) return;

        Minecraft mc = Minecraft.getInstance();
        Vec3 camera = event.getCamera().getPosition();
        Vec3 renderMarker = previousMarkerPosition == null
                ? markerPosition
                : previousMarkerPosition.lerp(markerPosition,
                        event.getPartialTick().getGameTimeDeltaPartialTick(false));
        if (renderMarker.distanceToSqr(camera) > MAX_MARKER_DISTANCE * MAX_MARKER_DISTANCE) return;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        Vec3 offset = renderMarker.subtract(camera);
        poseStack.translate(offset.x, offset.y, offset.z);

        VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(AIM_MARKER_RENDER_TYPE);
        double renderRadius = previousMarkerPosition == null
                ? markerRadius
                : previousMarkerRadius + (markerRadius - previousMarkerRadius)
                        * event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Matrix4fRing.draw(consumer, poseStack, renderRadius, event.getRenderTick());
        mc.renderBuffers().bufferSource().endBatch(AIM_MARKER_RENDER_TYPE);
        poseStack.popPose();
    }

    /** Isolated vertex code keeps the render event readable and avoids allocating a mesh each frame. */
    private static final class Matrix4fRing {
        private Matrix4fRing() {}

        private static void draw(VertexConsumer consumer, PoseStack poseStack, double radius, int tick) {
            var pose = poseStack.last().pose();
            double pulse = 1.0 + Math.sin(tick * 0.18) * 0.08;
            double r = radius * pulse;
            for (int i = 0; i < RING_SEGMENTS; i++) {
                double a0 = i * Math.PI * 2.0 / RING_SEGMENTS;
                double a1 = (i + 1) * Math.PI * 2.0 / RING_SEGMENTS;
                consumer.addVertex(pose, (float) (Math.cos(a0) * r), 0.02f, (float) (Math.sin(a0) * r))
                        .setColor(RING_RED, RING_GREEN, RING_BLUE, 255);
                consumer.addVertex(pose, (float) (Math.cos(a1) * r), 0.02f, (float) (Math.sin(a1) * r))
                        .setColor(RING_RED, RING_GREEN, RING_BLUE, 255);
            }

            float arm = (float) (r * 0.65);
            consumer.addVertex(pose, -arm, 0.03f, 0).setColor(RING_RED, RING_GREEN, RING_BLUE, 255);
            consumer.addVertex(pose, arm, 0.03f, 0).setColor(RING_RED, RING_GREEN, RING_BLUE, 255);
            consumer.addVertex(pose, 0, 0.03f, -arm).setColor(RING_RED, RING_GREEN, RING_BLUE, 255);
            consumer.addVertex(pose, 0, 0.03f, arm).setColor(RING_RED, RING_GREEN, RING_BLUE, 255);
            consumer.addVertex(pose, 0, 0.03f, 0).setColor(255, 220, 96, 255);
            consumer.addVertex(pose, 0, 1.6f, 0).setColor(255, 220, 96, 255);
        }
    }
}
