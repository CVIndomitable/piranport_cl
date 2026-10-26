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
import java.util.HashSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * Draws the predicted impact point of every tracked aerial bomb and shell.
 *
 * <p>The marker is derived from the projectile entity replicated by the server,
 * so it is visible to every client tracking that projectile.  No client-side
 * fire-control lock or owner-only state is involved; observers can use it to
 * dodge incoming fire as well.</p>
 *
 * <p>The ring is the still-unresolved dispersion: its radius is the remaining
 * flight distance times the dispersion angle, so it shrinks toward the impact
 * point as the projectile flies and collapses to the cross at landing.</p>
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public final class ProjectileLandingMarkerRenderer {
    private static final double MAX_DISTANCE = 256.0;
    private static final int MAX_STEPS = 240;
    private static final int SEGMENTS = 24;
    private static final int PREDICTION_INTERVAL = 4;
    private static final int MAX_PROJECTILES = 64;
    private static final double MIN_RING_RADIUS = 0.05;
    private static final float CROSS_ARM = 0.3f;
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
    /** 上一次重算时已见过的航弹实体 ID，用于「新航弹立即重算」。 */
    private static final Set<Integer> knownBombIds = new HashSet<>();
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
            if (!marker.source.isAlive()) continue;
            double radius = marker.source.position().distanceTo(marker.position) * marker.tanDispersion;
            poseStack.pushPose();
            Vec3 offset = marker.position.subtract(camera);
            poseStack.translate(offset.x, offset.y + 0.04, offset.z);
            drawRing(consumer, poseStack, radius, marker.red, marker.green, marker.blue);
            poseStack.popPose();
            drew = true;
        }
        if (drew) mc.renderBuffers().bufferSource().endBatch(MARKER);
    }

    private static void refreshMarkers(ClientLevel level, Vec3 camera) {
        long gameTime = level.getGameTime();
        boolean levelChanged = cachedLevel != level;
        List<Entity> bombs = new ArrayList<>();
        List<Entity> shells = new ArrayList<>();
        for (Entity entity : level.entitiesForRendering()) {
            boolean bomb = entity instanceof AerialBombEntity;
            if (!bomb && !(entity instanceof CannonProjectileEntity)) continue;
            if (!entity.isAlive() || entity.distanceToSqr(camera) > MAX_DISTANCE * MAX_DISTANCE) continue;
            (bomb ? bombs : shells).add(entity);
        }
        // 新出现的航弹立即重算，不等 4 tick 间隔：俯冲投弹点离水面只有两三格，航弹往往只活几个 tick，
        // 刚好落在两次重算之间就会整段没有落点标记。
        boolean newBomb = false;
        for (Entity bomb : bombs) {
            if (!knownBombIds.contains(bomb.getId())) {
                newBomb = true;
                break;
            }
        }
        boolean intervalElapsed = levelChanged || gameTime - lastPredictionTick >= PREDICTION_INTERVAL;
        if (!intervalElapsed && !newBomb) return;
        cachedLevel = level;
        lastPredictionTick = gameTime;
        MARKERS.clear();
        knownBombIds.clear();
        for (Entity bomb : bombs) {
            knownBombIds.add(bomb.getId());
        }
        // 航弹优先：俯冲炸弹投下后只活几个 tick，数量又远少于深海舰炮炮弹。
        // 若按实体遍历顺序与炮弹共享上限，舰队战里炮弹会先占满 MAX_PROJECTILES，航弹永远进不了列表。
        int count = 0;
        for (Entity bomb : bombs) {
            if (count >= MAX_PROJECTILES) break;
            if (addMarker(level, bomb)) count++;
        }
        for (Entity shell : shells) {
            if (count >= MAX_PROJECTILES) break;
            if (addMarker(level, shell)) count++;
        }
    }

    private static boolean addMarker(ClientLevel level, Entity entity) {
        Vec3 impact = predictImpact(level, entity);
        if (impact == null) return false;
        boolean bomb = entity instanceof AerialBombEntity;
        double dispersionDeg = bomb
                ? ((AerialBombEntity) entity).getDispersionDeg()
                : ((CannonProjectileEntity) entity).getDispersionDeg();
        double tanDispersion = Math.tan(Math.toRadians(Math.max(0.0, dispersionDeg)));
        MARKERS.add(new Marker(entity, impact, tanDispersion,
                255, bomb ? 190 : 80, bomb ? 40 : 32));
        return true;
    }

    private static Vec3 predictImpact(ClientLevel level, Entity entity) {
        Vec3 position = entity.position();
        Vec3 velocity = entity.getDeltaMovement();
        if (!finite(position) || !finite(velocity)) return null;
        // 速度尚未同步（刚生成、速度为 0）时不再直接丢弃：航弹受重力影响，仍能按竖直下落推算落点。
        // 否则敌方投弹点/弹速延迟一帧同步，短命的俯冲航弹就会整段没有落点标记。
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
        if (radius >= MIN_RING_RADIUS) {
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
        }
        consumer.addVertex(pose, -CROSS_ARM, 0.01f, 0).setColor(red, green, blue, 255);
        consumer.addVertex(pose, CROSS_ARM, 0.01f, 0).setColor(red, green, blue, 255);
        consumer.addVertex(pose, 0, 0.01f, -CROSS_ARM).setColor(red, green, blue, 255);
        consumer.addVertex(pose, 0, 0.01f, CROSS_ARM).setColor(red, green, blue, 255);
    }

    private static boolean finite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    /**
     * source 每帧取当前位置算圈半径（圈随弹丸实时收缩）；落点每 {@link #PREDICTION_INTERVAL} tick 重算一次。
     */
    private record Marker(Entity source, Vec3 position, double tanDispersion, int red, int green, int blue) {}
}
