package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piranport.aviation.AircraftLandingTimeline;
import com.piranport.entity.AircraftEntity;
import com.piranport.network.AircraftLandingVisualPayload;
import com.piranport.rigging.RiggingSocket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 回收实体删除后继续显示降落模型，不持有或返还任何物品。 */
public final class AircraftLandingVisualClientState {
    private static final Map<UUID, Landing> LANDINGS = new HashMap<>();
    private static ClientLevel level;

    private AircraftLandingVisualClientState() {}

    private static void cleanup() {
        ClientLevel now = Minecraft.getInstance().level;
        if (now != level) {
            LANDINGS.clear();
            level = now;
        }
        if (now == null) return;
        LANDINGS.values().removeIf(l -> {
            if (!l.timeline.holdsDeck(age(l, 0))) return true;
            var launcher = now.getEntity(l.packet.launcherId());
            if (!(launcher instanceof AbstractClientPlayer player)) return false;
            return !player.getUUID().equals(l.packet.launcherUuid()) || !player.isAlive()
                    || !RiggingClientState.isRenderedAsTransformed(player);
        });
    }

    public static void trigger(AircraftLandingVisualPayload packet) {
        cleanup();
        if (level == null || !validDuration(packet.approachTicks()) || !validDuration(packet.rollTicks())
                || !validDuration(packet.settleTicks()) || packet.targetSlot() < 0 || packet.targetSlot() >= 9
                || (packet.socketId() != RiggingSocket.DECK_1.ordinal()
                && packet.socketId() != RiggingSocket.DECK_2.ordinal())
                || !Double.isFinite(packet.sourceX()) || !Double.isFinite(packet.sourceY())
                || !Double.isFinite(packet.sourceZ()) || !Float.isFinite(packet.sourceYaw())
                || !Float.isFinite(packet.sourcePitch())) return;
        cancelSlot(packet.launcherUuid(), packet.targetSlot());
        Vec3 source = new Vec3(packet.sourceX(), packet.sourceY(), packet.sourceZ());
        float yaw = packet.sourceYaw();
        float pitch = packet.sourcePitch();
        // 客户端插值可能落后服务端，接住最后看见的位置，避免实体消失时先跳一段。
        var entity = level.getEntity(packet.aircraftId());
        if (entity instanceof AircraftEntity && entity.getUUID().equals(packet.aircraftUuid())) {
            float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
            source = entity.getPosition(partial);
            yaw = Mth.rotLerp(partial, entity.yRotO, entity.getYRot());
            pitch = Mth.lerp(partial, entity.xRotO, entity.getXRot());
        }
        LANDINGS.put(packet.aircraftUuid(), new Landing(packet, source, yaw, pitch));
    }

    private static boolean validDuration(int ticks) { return ticks >= 1 && ticks <= 100; }

    public static void cancelSlot(UUID launcher, int slot) {
        cleanup();
        LANDINGS.values().removeIf(l -> l.packet.launcherUuid().equals(launcher) && l.packet.targetSlot() == slot);
    }

    private static boolean matches(Landing landing, int playerId) {
        var player = level.getEntity(playerId);
        return landing.packet.launcherId() == playerId && player instanceof AbstractClientPlayer
                && player.getUUID().equals(landing.packet.launcherUuid());
    }

    private static float age(Landing landing, float partial) {
        return (float) (level.getGameTime() - landing.packet.startTick()) + partial;
    }

    public static boolean hasActiveDeck(int playerId) {
        cleanup();
        return level != null && LANDINGS.values().stream().anyMatch(l -> matches(l, playerId));
    }

    public static float deckLevelWeight(int playerId, float partial) {
        cleanup();
        if (level == null) return 0;
        float weight = 0;
        for (Landing landing : LANDINGS.values()) {
            if (matches(landing, playerId)) weight = Math.max(weight, landing.timeline.levelWeight(age(landing, partial)));
        }
        return weight;
    }

    public static boolean hidesEntity(AircraftEntity entity) {
        cleanup();
        Landing landing = LANDINGS.get(entity.getUUID());
        if (landing == null || landing.packet.aircraftId() != entity.getId()) return false;
        var launcher = level.getEntity(landing.packet.launcherId());
        return launcher instanceof AbstractClientPlayer player && !player.isInvisible()
                && RiggingClientState.isRenderedAsTransformed(player);
    }

    public static void renderDeckAircraft(int playerId, PoseStack poses, MultiBufferSource buffers,
                                          int light, float partial, boolean firstPerson) {
        cleanup();
        if (level == null) return;
        float deckPitch = RiggingDeckClientState.pitch(playerId, partial);
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        for (Landing landing : LANDINGS.values()) {
            if (!matches(landing, playerId)) continue;
            float age = age(landing, partial);
            if (!landing.timeline.showsAircraft(age)) continue;
            // 后续回收可能把原槽挤出展示位；滑行终点冻结，不随停放快照跳位或提前消失。
            RiggingSocket socket = RiggingSocket.byId(landing.packet.socketId());
            float parkZ = RiggingDeckPose.aircraftZ(socket, firstPerson) / 16;
            float touchZ = (firstPerson ? -RiggingDeckPose.EDGE_Z : RiggingDeckPose.EDGE_Z) / 16;
            poses.pushPose();
            RiggingSocketTransforms.applyDeckPose(poses, deckPitch, firstPerson);
            if (age < landing.timeline.approachTicks()) {
                float progress = landing.timeline.approach(age);
                Vector3f point = poses.last().pose().transformPosition(new Vector3f(
                        RiggingDeckPose.CENTER_X / 16, RiggingDeckPose.AIRCRAFT_Y / 16, touchZ));
                Vec3 target = camera.add(point.x, point.y, point.z);
                Vec3 position = landing.source.lerp(target, progress);
                Vector3f forward = poses.last().pose().transformDirection(new Vector3f(0, 0, firstPerson ? 1 : -1));
                float yaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
                float pitch = (float) Math.toDegrees(Math.atan2(-forward.y, Math.hypot(forward.x, forward.z)));
                float frameScale = poses.last().pose().transformDirection(new Vector3f(1, 0, 0)).length();
                PoseStack world = new PoseStack();
                world.translate(position.x - camera.x, position.y - camera.y, position.z - camera.z);
                world.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(progress, landing.yaw, yaw)));
                world.mulPose(Axis.XP.rotationDegrees(Mth.lerp(progress, landing.pitch, pitch)));
                AircraftRiggingModelRenderer.render(landing.packet.visualId(), world, buffers, light,
                        landing.timeline.scale(age, AircraftRiggingModelRenderer.DECK_SCALE * frameScale), false);
            } else {
                poses.translate(RiggingDeckPose.CENTER_X / 16, RiggingDeckPose.AIRCRAFT_Y / 16,
                        Mth.lerp(landing.timeline.roll(age), touchZ, parkZ));
                AircraftRiggingModelRenderer.render(landing.packet.visualId(), poses, buffers, light,
                        AircraftRiggingModelRenderer.DECK_SCALE, !firstPerson);
            }
            poses.popPose();
        }
    }

    private static final class Landing {
        final AircraftLandingVisualPayload packet;
        final AircraftLandingTimeline timeline;
        final Vec3 source;
        final float yaw;
        final float pitch;
        Landing(AircraftLandingVisualPayload packet, Vec3 source, float yaw, float pitch) {
            this.packet = packet;
            this.timeline = new AircraftLandingTimeline(packet.approachTicks(), packet.rollTicks(), packet.settleTicks());
            this.source = source;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
