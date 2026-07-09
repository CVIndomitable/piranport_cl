package com.piranport.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Client-only short animation state for transformed aircraft launch poses. */
public final class AircraftLaunchPoseClientState {
    private static final Map<Integer, LaunchPose> POSES = new HashMap<>();

    private AircraftLaunchPoseClientState() {}

    public static void trigger(int entityId, int skinId, int durationTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        int safeDuration = Mth.clamp(durationTicks, 6, 60);
        long now = mc.level.getGameTime();
        POSES.put(entityId, new LaunchPose(now, safeDuration, skinId));
        cleanup(now);
    }

    public static float intensity(int entityId, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return 0.0f;
        }
        LaunchPose pose = POSES.get(entityId);
        if (pose == null) {
            return 0.0f;
        }

        float age = (mc.level.getGameTime() + partialTick) - pose.startTick();
        float t = age / pose.durationTicks();
        if (t >= 1.0f) {
            POSES.remove(entityId);
            return 0.0f;
        }
        if (t <= 0.0f) {
            return 0.0f;
        }

        float value = t < 0.35f ? t / 0.35f : 1.0f - (t - 0.35f) / 0.65f;
        return Mth.clamp(value, 0.0f, 1.0f);
    }

    public static int skinId(int entityId, int fallbackSkinId) {
        LaunchPose pose = POSES.get(entityId);
        return pose != null && pose.skinId() > 0 ? pose.skinId() : fallbackSkinId;
    }

    private static void cleanup(long now) {
        Iterator<Map.Entry<Integer, LaunchPose>> iterator = POSES.entrySet().iterator();
        while (iterator.hasNext()) {
            LaunchPose pose = iterator.next().getValue();
            if (now - pose.startTick() > pose.durationTicks() + 20L) {
                iterator.remove();
            }
        }
    }

    private record LaunchPose(long startTick, int durationTicks, int skinId) {}
}
