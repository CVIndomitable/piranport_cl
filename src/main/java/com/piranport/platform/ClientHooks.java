package com.piranport.platform;

import com.piranport.entity.AircraftEntity;
import com.piranport.network.CannonImpactEffectPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.Objects;

/**
 * 公共逻辑访问客户端能力的入口。
 * 客户端入口在注册物品前安装实现；运行时使用普通接口调用，异常保留原始调用栈。
 */
public final class ClientHooks {
    private static volatile ClientBridge bridge = new NoopClientBridge();

    private ClientHooks() {}

    public static void install(ClientBridge clientBridge) {
        bridge = Objects.requireNonNull(clientBridge);
    }

    public static boolean isClient() {
        return bridge.isClient();
    }

    public static boolean hasShiftDown() {
        return bridge.hasShiftDown();
    }

    public static Player getClientPlayer() {
        return bridge.getClientPlayer();
    }

    public static String getClientPlayerName() {
        return bridge.getClientPlayerName();
    }

    public static long getClientGameTime() {
        return bridge.getClientGameTime();
    }

    public static void resetClientState() {
        bridge.resetClientState();
    }

    public static void initializeSkinCoreItemClient(Object consumer) {
        bridge.initializeSkinCoreItemClient(consumer);
    }

    public static boolean toggleArtilleryScope(Player player, ItemStack stack) {
        return bridge.toggleArtilleryScope(player, stack);
    }

    public static void appendWeaponCooldownTooltip(ItemStack stack, List<Component> tooltip) {
        bridge.appendWeaponCooldownTooltip(stack, tooltip);
    }

    public static void handleTorpedoGuidanceState(boolean active, int entityId) {
        bridge.handleTorpedoGuidanceState(active, entityId);
    }

    public static void handleReconState(boolean active, int entityId) {
        bridge.handleReconState(active, entityId);
    }

    public static void triggerCameraShake(float intensity, int durationTicks) {
        bridge.triggerCameraShake(intensity, durationTicks);
    }

    public static void triggerAircraftLaunchPose(int entityId, int skinId, int durationTicks) {
        bridge.triggerAircraftLaunchPose(entityId, skinId, durationTicks);
    }

    public static void spawnCannonImpactEffect(CannonImpactEffectPayload payload) {
        bridge.spawnCannonImpactEffect(payload);
    }

    public static void updateAswSonar(int aircraftEntityId, List<Integer> detectedEntityIds) {
        bridge.updateAswSonar(aircraftEntityId, detectedEntityIds);
    }

    public static void setFireControlTargets(List<UUID> targetUUIDs) {
        bridge.setFireControlTargets(targetUUIDs);
    }

    public static void displayClientMessage(Component message) {
        bridge.displayClientMessage(message);
    }

    public static void displayClientMessage(Component message, boolean overlay) {
        bridge.displayClientMessage(message, overlay);
    }

    public static void setTitle(Component title) {
        bridge.setTitle(title);
    }

    public static void playSound(SoundEvent sound, float volume, float pitch) {
        bridge.playSound(sound, volume, pitch);
    }

    public static void openDungeonContinueScreen(BlockPos lecternPos, String stageName, int clearedNodeCount) {
        bridge.openDungeonContinueScreen(lecternPos, stageName, clearedNodeCount);
    }

    public static void setDebugEnabledClient(boolean enabled) {
        bridge.setDebugEnabledClient(enabled);
    }

    public static void setHitDisplayEnabledClient(boolean enabled) {
        bridge.setHitDisplayEnabledClient(enabled);
    }

    public static void setTestModeClient(boolean enabled) {
        bridge.setTestModeClient(enabled);
    }

    public static void setServerSolverStats(int ternaryIters, int newtonIters) {
        bridge.setServerSolverStats(ternaryIters, newtonIters);
    }

    public static void setServerSolverStats(int ternaryIters, int newtonIters, long totalUs, double verticalError, double horizontalError, double angleDeg) {
        bridge.setServerSolverStats(ternaryIters, newtonIters, totalUs, verticalError, horizontalError, angleDeg);
    }

    public static boolean isReconEntity(int entityId) {
        return bridge.isReconEntity(entityId);
    }

    public static boolean isInReconMode() {
        return bridge.isInReconMode();
    }

    /** 本地玩家是否正在线导该实体。服务端为 noop 实现恒 false。 */
    public static boolean isGuidingTorpedo(int entityId) {
        return bridge.isGuidingTorpedo(entityId);
    }

    public static void openTownScrollScreen() {
        bridge.openTownScrollScreen();
    }

    public static void openDungeonResultScreen(String stageName, long timeMillis, boolean isFirstClear, List<String> rewardNames) {
        bridge.openDungeonResultScreen(stageName, timeMillis, isFirstClear, rewardNames);
    }

    public static void openDungeonResultScreen(String stageName, long timeMillis, boolean isFirstClear,
                                               List<String> rewardNames, int kills) {
        bridge.openDungeonResultScreen(stageName, timeMillis, isFirstClear, rewardNames, kills);
    }

    /** 《副本/00》结语由服务端下发（DungeonResultPayload.ending）。 */
    public static void openDungeonResultScreen(String stageName, long timeMillis, boolean isFirstClear,
                                               List<String> rewardNames, int kills, String ending) {
        bridge.openDungeonResultScreen(stageName, timeMillis, isFirstClear, rewardNames, kills, ending);
    }

    public static void openDungeonReviveScreen() {
        bridge.openDungeonReviveScreen();
    }

    public static void updateDungeonNode(String nodeId) {
        bridge.updateDungeonNode(nodeId);
    }

    public static void setDungeonState(String stageName, String nodeId, long elapsedMillis) {
        bridge.setDungeonState(stageName, nodeId, elapsedMillis);
    }

    public static void updateDungeonBossOverlay(String bossName, String shipType, String chapter,
                                                int segment, float health, float maxHealth,
                                                boolean visible, boolean quietBattlefield) {
        bridge.updateDungeonBossOverlay(bossName, shipType, chapter, segment,
                health, maxHealth, visible, quietBattlefield);
    }

    public static boolean shouldAircraftGlow(AircraftEntity aircraft) {
        return bridge.shouldAircraftGlow(aircraft);
    }

    public static int getAircraftGlowColor(AircraftEntity aircraft, int fallbackColor) {
        return bridge.getAircraftGlowColor(aircraft, fallbackColor);
    }
}
