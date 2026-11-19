package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.rigging.RiggingLoadout;
import com.piranport.rigging.RiggingMount;
import com.piranport.rigging.RiggingSocket;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;

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

    /** 甲板还在播放动画时，快照可能已恢复双臂分配；右舷仍只显示主手。 */
    public static boolean hidesSideMount(RiggingLoadout loadout, RiggingMount mount, boolean showDeck) {
        return showDeck && switch (mount.socket()) {
            case SIDE_RIGHT_1 -> true;
            case SIDE_RIGHT_2 -> !loadout.isMainHandSideMount(mount);
            default -> false;
        };
    }

    public static boolean showMainHandSupport(RiggingLoadout loadout, boolean showDeck) {
        return showDeck && loadout.mounts().stream().anyMatch(loadout::isMainHandSideMount);
    }

    public static float pitch(int playerId, float partialTick) {
        return RiggingDeckPose.pitchDegrees(
                AircraftLaunchVisualClientState.deckDeployment(playerId, partialTick),
                AircraftLandingVisualClientState.deckLevelWeight(playerId, partialTick),
                ModEquipmentConfig.RIGGING_DECK_IDLE_PITCH.get().floatValue(),
                ModEquipmentConfig.RIGGING_DECK_LAUNCH_PITCH.get().floatValue());
    }

    /** 接机期间清空停放位；放飞飞机还在板上时，已经补到前位的静态飞机暂留后位。 */
    public static @Nullable RiggingMount parkedMount(int playerId, RiggingLoadout loadout,
                                                    RiggingMount mount, float partialTick) {
        if (mount.socket() != RiggingSocket.DECK_1 && mount.socket() != RiggingSocket.DECK_2) return mount;
        if (loadout.hidesParkedAircraft(AircraftLandingVisualClientState.hasActiveDeck(playerId))
                || AircraftLaunchVisualClientState.shouldHideMount(playerId, mount)) return null;
        if (!AircraftLaunchVisualClientState.usesForwardSlot(playerId, partialTick)) return mount;
        if (mount.socket() == RiggingSocket.DECK_1) {
            return new RiggingMount(RiggingSocket.DECK_2, mount.category(), mount.stack(), mount.sourceSlot());
        }
        for (RiggingMount other : loadout.mounts()) {
            if (other.socket() == RiggingSocket.DECK_1
                    && !AircraftLaunchVisualClientState.shouldHideMount(playerId, other)) return null;
        }
        return mount;
    }

    public static void renderAircraft(int playerId, PoseStack poses, MultiBufferSource buffers,
                                      int light, float partialTick, boolean firstPerson) {
        AircraftLaunchVisualClientState.renderDeckAircraft(playerId, poses, buffers, light, partialTick, firstPerson);
        AircraftLandingVisualClientState.renderDeckAircraft(playerId, poses, buffers, light, partialTick, firstPerson);
    }
}
