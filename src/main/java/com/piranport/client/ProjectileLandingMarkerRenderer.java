package com.piranport.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.piranport.PiranPort;
import com.piranport.entity.AerialBombEntity;
import com.piranport.entity.CannonProjectileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Draws the predicted impact point of every tracked aerial bomb and shell.
 *
 * <p>The marker is derived from the projectile entity replicated by the server,
 * so it is visible to every client tracking that projectile.  No client-side
 * fire-control lock or owner-only state is involved; observers can use it to
 * dodge incoming fire as well.</p>
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public final class ProjectileLandingMarkerRenderer {
    private static final double MAX_DISTANCE = 256.0;
    private static final int MAX_STEPS = 240;
    private static final int SEGMENTS = 24;
    private static final int PREDICTION_INTERVAL = 4;
    private static final int MAX_PROJECTILES = 64;
    private static final RenderType MARKER = RenderType.create(
            "piranport_projectile_landing_marker",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.DEBUG_LINES,
            256,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2.0)))
                    .createCompositeState(false));
    private static final List<Marker> MARKERS = new ArrayList<>();
    private static ClientLevel cachedLevel;
    private static long lastPredictionTick = Long.MIN_VALUE;

    private ProjectileLandingMarkerRenderer() {}

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        Vec3 camera = event.getCamera().getPosition();
        refreshMarkers(level, camera);
        PoseStack poseStack = event.getPoseStack();
        VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(MARKER);
        boolean drew = false;
        for (Marker marker : MARKERS) {
            if (marker.position.distanceToSqr(camera) > MAX_DISTANCE * MAX_DISTANCE) continue;
            poseStack.pushPose();
            Vec3 offset = marker.position.subtract(camera);
            poseStack.translate(offset.x, offset.y + 0.04, offset.z);
            drawRing(consumer, poseStack, marker.radius, marker.red, marker.green, marker.blue);
            poseStack.popPose();
            drew = true;
        }
        if (drew) mc.renderBuffers().bufferSource().endBatch(MARKER);
    }

    private static void refreshMarkers(ClientLevel level, Vec3 camera) {
        long gameTime = level.getGameTime();
        if (cachedLevel == level && gameTime - lastPredictionTick < PREDICTION_INTERVAL) return;
        cachedLevel = level;
        lastPredictionTick = gameTime;
        MARKERS.clear();
        int count = 0;
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof AerialBombEntity) && !(entity instanceof CannonProjectileEntity)) continue;
            if (!entity.isAlive() || entity.distanceToSqr(camera) > MAX_DISTANCE * MAX_DISTANCE) continue;
            Vec3 impact = predictImpact(level, entity);
            if (impact == null) continue;
            boolean bomb = entity instanceof AerialBombEntity;
            MARKERS.add(new Marker(impact, bomb ? 0.85 : 0.65,
                    255, bomb ? 190 : 80, bomb ? 40 : 32));
            if (++count >= MAX_PROJECTILES) break;
        }
    }

    private static Vec3 predictImpact(ClientLevel level, Entity entity) {
        Vec3 position = entity.position();
        Vec3 velocity = entity.getDeltaMovement();
        if (!finite(position) || !finite(velocity) || velocity.lengthSqr() < 1.0e-8) return null;
        double gravity = entity instanceof AerialBombEntity bomb
                ? bomb.getMarkerGravity()
                : ((CannonProjectileEntity) entity).getMarkerGravity();
        double customDrag = entity instanceof CannonProjectileEntity shell
                ? shell.getMarkerDragCoeff() : 0.0;
        for (int step = 0; step < MAX_STEPS; step++) {
            if (customDrag > 0.0) velocity = velocity.scale(1.0 / (1.0 + customDrag));
            Vec3 next = position.add(velocity);
            var hit = level.clip(new ClipContext(position, next,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, entity));
            if (hit.getType() != HitResult.Type.MISS) return hit.getLocation();
            position = next;
            velocity = velocity.scale(0.99).add(0.0, -gravity, 0.0);
            if (position.y < level.getMinBuildHeight()) return position;
        }
        return position;
    }

    private static void drawRing(VertexConsumer consumer, PoseStack poseStack,
                                 double radius, int red, int green, int blue) {
        var pose = poseStack.last().pose();
        for (int i = 0; i < SEGMENTS; i++) {
            double a0 = i * Math.PI * 2.0 / SEGMENTS;
            double a1 = (i + 1) * Math.PI * 2.0 / SEGMENTS;
            consumer.addVertex(pose, (float) (Math.cos(a0) * radius), 0.0f,
                            (float) (Math.sin(a0) * radius))
                    .setColor(red, green, blue, 255);
            consumer.addVertex(pose, (float) (Math.cos(a1) * radius), 0.0f,
                            (float) (Math.sin(a1) * radius))
                    .setColor(red, green, blue, 255);
        }
        float arm = (float) (radius * 0.65);
        consumer.addVertex(pose, -arm, 0.01f, 0).setColor(red, green, blue, 255);
        consumer.addVertex(pose, arm, 0.01f, 0).setColor(red, green, blue, 255);
        consumer.addVertex(pose, 0, 0.01f, -arm).setColor(red, green, blue, 255);
        consumer.addVertex(pose, 0, 0.01f, arm).setColor(red, green, blue, 255);
    }

    private static boolean finite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    private record Marker(Vec3 position, double radius, int red, int green, int blue) {}
}
