package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.aviation.AircraftLaunchTimeline;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.entity.AircraftEntity;
import com.piranport.network.AircraftLaunchVisualPayload;
import com.piranport.rigging.RiggingMount;
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

/** 玩家飞机视觉时间轴；不修改实际实体的状态或坐标。 */
public final class AircraftLaunchVisualClientState {
    private static final Map<UUID, Flight> FLIGHTS = new HashMap<>();
    private static ClientLevel level;
    private AircraftLaunchVisualClientState() {}

    private static void cleanup() {
        ClientLevel now = Minecraft.getInstance().level;
        if (now != level) { FLIGHTS.clear(); level = now; }
        if (now == null) return;
        long tick = now.getGameTime();
        FLIGHTS.values().removeIf(f -> {
            if (tick - f.packet.startTick() > f.timeline.durationTicks() + 20L) return true;
            var entity = now.getEntity(f.packet.aircraftId());
            if (entity != null) {
                if (!entity.getUUID().equals(f.packet.aircraftUuid()) || entity.isRemoved()) return true;
                f.entitySeen = true;
            }
            return f.entitySeen && entity == null;
        });
    }

    public static void trigger(AircraftLaunchVisualPayload p) {
        cleanup();
        if (level == null || p.deployTicks() < 1 || p.deployTicks() > 100 || p.rollTicks() < 1
                || p.rollTicks() > 100 || p.flightTicks() < 1 || p.flightTicks() > 100
                || (p.socketId() != RiggingSocket.DECK_1.ordinal() && p.socketId() != RiggingSocket.DECK_2.ordinal())) return;
        AircraftLandingVisualClientState.cancelSlot(p.launcherUuid(), p.sourceSlot());
        FLIGHTS.put(p.aircraftUuid(), new Flight(p));
    }

    private static float age(Flight f, float partialTick) {
        return (float) (level.getGameTime() - f.packet.startTick()) + partialTick;
    }

    public static boolean hasActiveDeck(int launcherId) {
        cleanup();
        return level != null && FLIGHTS.values().stream().anyMatch(f -> validLauncher(f, launcherId)
                && age(f, 0) < f.timeline.durationTicks());
    }

    private static boolean validLauncher(Flight f, int id) {
        var entity = level.getEntity(id);
        return f.packet.launcherId() == id && (entity == null || entity.getUUID().equals(f.packet.launcherUuid()));
    }

    public static float deckDeployment(int launcherId, float partialTick) {
        cleanup();
        if (level == null) return 0;
        float result = 0;
        for (Flight f : FLIGHTS.values()) {
            if (!validLauncher(f, launcherId)) continue;
            float age = age(f, partialTick);
            if (age < f.timeline.durationTicks()) {
                result = Math.max(result, f.timeline.deckDeployment(age));
            }
        }
        return result;
    }

    public static boolean shouldHideMount(int launcherId, RiggingMount mount) {
        cleanup();
        if (level == null || mount.sourceSlot() < 0) return false;
        var launcher = level.getEntity(launcherId);
        if (!(launcher instanceof net.minecraft.client.player.AbstractClientPlayer player)) return false;
        long fingerprint = RiggingClientState.resolveLoadout(player).fingerprint();
        for (Flight f : FLIGHTS.values()) {
            if (validLauncher(f, launcherId) && f.packet.sourceSlot() == mount.sourceSlot()
                    && f.packet.snapshotFingerprint() == fingerprint
                    && age(f, 0) < f.timeline.durationTicks()) return true;
        }
        return false;
    }

    public static void renderDeckAircraft(int launcherId, PoseStack poses, MultiBufferSource buffers,
                                          int light, float partialTick, boolean firstPerson) {
        cleanup();
        if (level == null) return;
        float pitch = RiggingDeckClientState.pitch(launcherId, partialTick);
        for (Flight f : FLIGHTS.values()) {
            if (!validLauncher(f, launcherId)) continue;
            float age = age(f, partialTick);
            if (age < 0 || age >= f.timeline.departureTick()) continue;
            float edgeZ = (firstPerson ? RiggingDeckPose.EDGE_Z : -RiggingDeckPose.EDGE_Z) / 16;
            poses.pushPose();
            RiggingSocketTransforms.applyDeckPose(poses, pitch, firstPerson);
            // 离板点和朝向取实际甲板姿态，收放同时发生时也不会从错误高度交接。
            Vector3f p = poses.last().pose().transformPosition(new Vector3f(
                    RiggingDeckPose.CENTER_X / 16, RiggingDeckPose.AIRCRAFT_Y / 16, edgeZ));
            Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            f.position = camera.add(p.x, p.y, p.z);
            f.scale = AircraftRiggingModelRenderer.DECK_SCALE
                    * poses.last().pose().transformDirection(new Vector3f(1, 0, 0)).length();
            Vector3f forward = poses.last().pose().transformDirection(new Vector3f(0, 0, firstPerson ? 1 : -1));
            f.yaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
            f.pitch = (float) Math.toDegrees(Math.atan2(-forward.y, Math.hypot(forward.x, forward.z)));
            float startZ = RiggingDeckPose.aircraftZ(f.packet.socketId() == RiggingSocket.DECK_1.ordinal()) / 16;
            poses.translate(RiggingDeckPose.CENTER_X / 16, RiggingDeckPose.AIRCRAFT_Y / 16,
                    Mth.lerp(f.timeline.roll(age), startZ, edgeZ));
            AircraftRiggingModelRenderer.render(f.packet.visualId(), poses, buffers, light,
                    AircraftRiggingModelRenderer.DECK_SCALE, !firstPerson);
            poses.popPose();
        }
    }

    static Flight flight(AircraftEntity entity, float partialTick) {
        cleanup();
        if (level == null) return null;
        Flight f = FLIGHTS.get(entity.getUUID());
        if (f == null || f.packet.aircraftId() != entity.getId()
                || age(f, partialTick) >= f.timeline.durationTicks()) return null;
        if (age(f, partialTick) >= f.timeline.departureTick()) return f.position == null ? null : f;
        var launcher = level.getEntity(f.packet.launcherId());
        if (!(launcher instanceof AbstractClientPlayer player) || player.isInvisible()
                || !validLauncher(f, player.getId()) || !RiggingClientState.isRenderedAsTransformed(player)) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCameraEntity() == player && mc.options.getCameraType().isFirstPerson()
                && !ModEquipmentConfig.RIGGING_FP_ENABLED.get()) return null;
        return f;
    }

    static float flightAge(Flight f, float partialTick) { return age(f, partialTick); }
    static final class Flight {
        final AircraftLaunchVisualPayload packet;
        final AircraftLaunchTimeline timeline;
        Vec3 position;
        float yaw;
        float pitch;
        float scale = AircraftRiggingModelRenderer.DECK_SCALE;
        boolean entitySeen;
        Flight(AircraftLaunchVisualPayload p) {
            packet = p;
            timeline = new AircraftLaunchTimeline(p.deployTicks(), p.rollTicks(), p.flightTicks());
        }
    }
}
