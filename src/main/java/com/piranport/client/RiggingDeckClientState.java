package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.rigging.RiggingLoadout;
import com.piranport.rigging.RiggingMount;
import net.minecraft.client.renderer.MultiBufferSource;

/** 甲板、静态飞机和收放动画共用同一次姿态规则。 */
public final class RiggingDeckClientState {
    private RiggingDeckClientState() {}

    public static boolean hasActiveAnimation(int playerId) {
        return AircraftLaunchVisualClientState.hasActiveDeck(playerId)
                || AircraftLandingVisualClientState.hasActiveDeck(playerId);
    }

    public static boolean showDeck(RiggingLoadout loadout, int playerId) {
        return loadout.hasAircraft() || hasActiveAnimation(playerId);
    }

    public static float pitch(int playerId, float partialTick) {
        return RiggingDeckPose.pitchDegrees(
                AircraftLaunchVisualClientState.deckDeployment(playerId, partialTick),
                AircraftLandingVisualClientState.deckLevelWeight(playerId, partialTick),
                ModEquipmentConfig.RIGGING_DECK_IDLE_PITCH.get().floatValue(),
                ModEquipmentConfig.RIGGING_DECK_LAUNCH_PITCH.get().floatValue());
    }

    public static boolean shouldHideMount(int playerId, RiggingMount mount) {
        return AircraftLaunchVisualClientState.shouldHideMount(playerId, mount)
                || AircraftLandingVisualClientState.shouldHideMount(playerId, mount);
    }

    public static void renderAircraft(int playerId, PoseStack poses, MultiBufferSource buffers,
                                      int light, float partialTick, boolean firstPerson) {
        AircraftLaunchVisualClientState.renderDeckAircraft(playerId, poses, buffers, light, partialTick, firstPerson);
        AircraftLandingVisualClientState.renderDeckAircraft(playerId, poses, buffers, light, partialTick, firstPerson);
    }
}
