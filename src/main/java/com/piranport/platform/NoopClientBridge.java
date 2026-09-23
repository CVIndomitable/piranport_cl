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

/** 独立服务器上的空实现，不加载任何客户端类。 */
class NoopClientBridge implements ClientBridge {
    @Override
    public boolean isClient() { return false; }

    @Override
    public boolean hasShiftDown() { return false; }

    @Override
    public Player getClientPlayer() { return null; }

    @Override
    public String getClientPlayerName() { return null; }

    @Override
    public long getClientGameTime() { return -1L; }

    @Override
    public void resetClientState() {  }

    @Override
    public boolean isHighlightEnabled() { return false; }

    @Override
    public void initializeSkinCoreItemClient(Object consumer) {  }

    @Override
    public boolean toggleArtilleryScope(Player player, ItemStack stack) { return false; }

    @Override
    public void appendWeaponCooldownTooltip(ItemStack stack, List<Component> tooltip) {  }

    @Override
    public void handleTorpedoGuidanceState(boolean active, int entityId) {  }

    @Override
    public void handleReconState(boolean active, int entityId) {  }

    @Override
    public void triggerCameraShake(float intensity, int durationTicks) {  }

    @Override
    public void triggerAircraftLaunchPose(int entityId, int skinId, int durationTicks) {  }

    @Override
    public void spawnCannonImpactEffect(CannonImpactEffectPayload payload) {  }

    @Override
    public void updateAswSonar(int aircraftEntityId, List<Integer> detectedEntityIds) {  }

    @Override
    public void setFireControlTargets(List<UUID> targetUUIDs) {  }

    @Override
    public void setFcRadarSnapLimit(double limitBlocks) {  }

    @Override
    public void displayClientMessage(Component message) {  }

    @Override
    public void displayClientMessage(Component message, boolean overlay) {  }

    @Override
    public void setTitle(Component title) {  }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {  }

    @Override
    public void openDungeonContinueScreen(BlockPos lecternPos, String stageName, int clearedNodeCount) {  }

    @Override
    public void setDebugEnabledClient(boolean enabled) {  }

    @Override
    public void setTestModeClient(boolean enabled) {  }

    @Override
    public void setServerSolverStats(int ternaryIters, int newtonIters) {  }

    @Override
    public void setServerSolverStats(int ternaryIters, int newtonIters, long totalUs, double verticalError, double horizontalError, double angleDeg) {  }

    @Override
    public boolean isReconEntity(int entityId) { return false; }

    @Override
    public boolean isInReconMode() { return false; }

    @Override
    public void openTownScrollScreen() {  }

    @Override
    public void openDungeonResultScreen(String stageName, long timeMillis, boolean isFirstClear, List<String> rewardNames) {  }

    @Override
    public void openDungeonResultScreen(String stageName, long timeMillis, boolean isFirstClear,
                                        List<String> rewardNames, int kills) {  }

    @Override
    public void openDungeonReviveScreen() {  }

    @Override
    public void updateDungeonNode(String nodeId) {  }

    @Override
    public void setDungeonState(String stageName, String nodeId, long timerStartMillis) {  }

    @Override
    public void updateDungeonBossOverlay(String bossName, String shipType, String chapter,
                                         int segment, float health, float maxHealth,
                                         boolean visible, boolean quietBattlefield) {  }

    @Override
    public boolean shouldAircraftGlow(AircraftEntity aircraft) { return false; }

    @Override
    public int getAircraftGlowColor(AircraftEntity aircraft, int fallbackColor) { return fallbackColor; }

}
