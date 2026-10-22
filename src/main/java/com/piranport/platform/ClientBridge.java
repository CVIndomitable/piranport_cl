package com.piranport.platform;

import com.piranport.entity.AircraftEntity;
import com.piranport.network.CannonImpactEffectPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
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

    void displayClientMessage(Component message);

    void displayClientMessage(Component message, boolean overlay);

    void setTitle(Component title);

    void playSound(SoundEvent sound, float volume, float pitch);

    void openDungeonContinueScreen(BlockPos lecternPos);

    void setDebugEnabledClient(boolean enabled);

    default void setHitDisplayEnabledClient(boolean enabled) { }

    void setTestModeClient(boolean enabled);

    void setServerSolverStats(int ternaryIters, int newtonIters);

    void setServerSolverStats(int ternaryIters, int newtonIters, long totalUs, double verticalError, double horizontalError, double angleDeg);

    boolean isReconEntity(int entityId);

    boolean isInReconMode();

    /** 本地玩家当前是否正在线导指定实体。方案4 客户端权威的位置判据。 */
    boolean isGuidingTorpedo(int entityId);

    void openTownScrollScreen();

    /** 打开创造模式弹药库的勾选界面。 */
    void openCreativeAmmoCacheScreen(InteractionHand hand);

    void openDungeonResultScreen(String stageName, long timeMillis, boolean isFirstClear, List<String> rewardNames);

    void openDungeonResultScreen(String stageName, long timeMillis, boolean isFirstClear,
                                 List<String> rewardNames, int kills);

    void openDungeonResultScreen(String stageName, long timeMillis, boolean isFirstClear,
                                 List<String> rewardNames, int kills, String ending);

    /** 出口传送门的"下界门式演出"开关；时长与传送时机都由服务端决定，客户端只做表现。 */
    void setPortalTransit(boolean active);

    /** 节点通关：右上角滑下进度式横幅（《副本/19》2026-10-22 补充，取代结算页弹窗）。 */
    void showNodeClearedToast(String stageName, String nodeName, boolean firstClear);


    void updateDungeonNode(String nodeId);

    void setDungeonState(String stageName, String nodeName, long elapsedMillis,
                          String objectiveKey, String sceneKey);

    void setCaptureProgress(float percent);

    void setBranchDoors(UUID instanceId, String fromNode,
                        List<com.piranport.dungeon.event.BranchDoors.Door> doors);

    void updateDungeonBossOverlay(String bossName, String shipType, String chapter,
                                  int segment, float health, float maxHealth,
                                  boolean visible, boolean quietBattlefield);

    boolean shouldAircraftGlow(AircraftEntity aircraft);

    int getAircraftGlowColor(AircraftEntity aircraft, int fallbackColor);

}
