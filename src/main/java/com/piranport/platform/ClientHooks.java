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

    public static boolean isHighlightEnabled() {
        return bridge.isHighlightEnabled();
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

    /** 服务端下发火控雷达准星吸附的半径上限（格）。服务端侧是 no-op。 */
    public static void setFcRadarSnapLimit(double limitBlocks) {
        bridge.setFcRadarSnapLimit(limitBlocks);
    }

    /**
     * 把原始鼠标灵敏度换成真正参与转向换算的那个值。
     *
     * <p>WHY 要有这层转发：原版 {@code MouseHandler#turnPlayer} 在乘系数之前会先把灵敏度
     * 过一个 NeoForge 钩子（{@code ClientHooks.getTurnPlayerValues}），有 mod 改写它时
     * 实际生效的灵敏度就不是选项里的那个数。本方法让吸附走和原版鼠标完全相同的取值路径，
     * 否则装了改灵敏度的 mod 时，吸附速度会与鼠标手感对不上。
     *
     * <p>服务端侧返回入参本身（那边根本没有客户端选项）。
     */
    public static double turnPlayerSensitivity(double rawSensitivity) {
        return bridge.turnPlayerSensitivity(rawSensitivity);
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

    public static void openDungeonReviveScreen() {
        bridge.openDungeonReviveScreen();
    }

    public static void updateDungeonNode(String nodeId) {
        bridge.updateDungeonNode(nodeId);
    }

    public static void setDungeonState(String stageName, String nodeId, long timerStartMillis) {
        bridge.setDungeonState(stageName, nodeId, timerStartMillis);
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
