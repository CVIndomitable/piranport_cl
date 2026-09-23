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

/** 客户端能力契约；签名只使用两端均可加载的类型。 */
public interface ClientBridge {
    boolean isClient();

    boolean hasShiftDown();

    Player getClientPlayer();

    String getClientPlayerName();

    long getClientGameTime();

    void resetClientState();

    boolean isHighlightEnabled();

    void initializeSkinCoreItemClient(Object consumer);

    boolean toggleArtilleryScope(Player player, ItemStack stack);

    void appendWeaponCooldownTooltip(ItemStack stack, List<Component> tooltip);

    void handleTorpedoGuidanceState(boolean active, int entityId);

    void handleReconState(boolean active, int entityId);

    void triggerCameraShake(float intensity, int durationTicks);

    void triggerAircraftLaunchPose(int entityId, int skinId, int durationTicks);

    void spawnCannonImpactEffect(CannonImpactEffectPayload payload);

    void updateAswSonar(int aircraftEntityId, List<Integer> detectedEntityIds);

    void setFireControlTargets(List<UUID> targetUUIDs);

    void setFcRadarSnapLimit(double limitBlocks);

    double turnPlayerSensitivity(double rawSensitivity);

    void displayClientMessage(Component message);

    void displayClientMessage(Component message, boolean overlay);

    void setTitle(Component title);

    void playSound(SoundEvent sound, float volume, float pitch);

    void openDungeonContinueScreen(BlockPos lecternPos, String stageName, int clearedNodeCount);

    void setDebugEnabledClient(boolean enabled);

    void setTestModeClient(boolean enabled);

    void setServerSolverStats(int ternaryIters, int newtonIters);

    void setServerSolverStats(int ternaryIters, int newtonIters, long totalUs, double verticalError, double horizontalError, double angleDeg);

    boolean isReconEntity(int entityId);

    boolean isInReconMode();

    void openTownScrollScreen();

    void openDungeonResultScreen(String stageName, long timeMillis, boolean isFirstClear, List<String> rewardNames);

    void openDungeonResultScreen(String stageName, long timeMillis, boolean isFirstClear,
                                 List<String> rewardNames, int kills);

    void openDungeonReviveScreen();

    void updateDungeonNode(String nodeId);

    void setDungeonState(String stageName, String nodeId, long timerStartMillis);

    void updateDungeonBossOverlay(String bossName, String shipType, String chapter,
                                  int segment, float health, float maxHealth,
                                  boolean visible, boolean quietBattlefield);

    boolean shouldAircraftGlow(AircraftEntity aircraft);

    int getAircraftGlowColor(AircraftEntity aircraft, int fallbackColor);

}
