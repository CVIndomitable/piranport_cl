package com.piranport.dungeon.event;

import com.piranport.PiranPort;
import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.entity.DungeonPortalEntity;
import com.piranport.dungeon.entity.LootShipEntity;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.instance.NodeBattleField;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.key.DungeonProgress;
import com.piranport.dungeon.network.DungeonResultPayload;
import com.piranport.dungeon.network.DungeonStatePayload;
import com.piranport.dungeon.network.PlayerDiedInDungeonPayload;
import com.piranport.dungeon.saved.DungeonLeaderboard;
import com.piranport.dungeon.saved.DungeonSavedData;
import com.piranport.dungeon.script.DungeonScriptManager;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Central event handler for the dungeon system.
 * Handles death, logout, data reload, and node transition logic.
 *
 * <p>整合版副本系统总体设计 2026-09-07 修订口径：
 * <ul>
 *   <li>§2.2 钥匙插在讲台上，玩家不携带进副本（阶段 2 起切换为读讲台 BE）</li>
 *   <li>§3.1 联机大厅/队长机制已作废（副本/10），所有玩家平等</li>
 *   <li>§3.3 死亡回门口 lecternPos（不读 playerCheckpoints，记录点仅 ContinueScreen 用）</li>
 *   <li>§3.3 不死图腾按原版逻辑在受致命伤害时结算（不取消事件）</li>
 *   <li>§3.4 副本永不自动删除（SUSPENDED 自动清理已移除）</li>
 * </ul>
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class DungeonEventHandler {

    public static final ResourceKey<Level> DUNGEON_DIMENSION =
            ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "dungeon"));

    /**
     * Gets the dungeon dimension level, or null if not available.
     */
    public static ServerLevel getDungeonLevel(MinecraftServer server) {
        return server.getLevel(DUNGEON_DIMENSION);
    }

    /**
     * Checks if a player is currently in the dungeon dimension.
     */
    public static boolean isInDungeon(ServerPlayer player) {
        return player.level().dimension().equals(DUNGEON_DIMENSION);
    }

    // ===== Key Authority Helpers (整合版 §2.2：钥匙插在讲台上) =====

    /**
     * 按玩家 UUID 反查其当前参与的第一个 ACTIVE 实例。
     * 整合版 §2.2：玩家不再持有钥匙，身份归属以"玩家加入实例"为权威。
     */
    private static DungeonInstance findActiveInstanceForPlayer(DungeonInstanceManager mgr, UUID playerUuid) {
        for (DungeonInstance inst : mgr.getAllInstances()) {
            if (inst.getState() != DungeonInstance.State.ACTIVE) continue;
            if (inst.getPlayerUuids().contains(playerUuid)) return inst;
        }
        return null;
    }

    /**
     * 从副本实例对应的讲台 BE 读取钥匙 ItemStack。
     * 整合版 §2.2：钥匙权威源是讲台 BE（讲台在 instance.lecternDimension 维度的 instance.lecternPos 位置），
     * 不再扫玩家背包。读到的钥匙其 DUNGEON_INSTANCE_ID 应等于 instance.getInstanceId()。
     *
     * @return BE 中的钥匙；若讲台缺失或 BE 中无钥匙，返回 ItemStack.EMPTY
     */
    private static ItemStack readKeyFromLectern(DungeonInstance instance, MinecraftServer server) {
        if (instance.getLecternPos() == null || instance.getLecternDimension() == null) {
            return ItemStack.EMPTY;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(instance.getLecternDimension());
        if (parsed == null) return ItemStack.EMPTY;
        ServerLevel lecternLevel = server.getLevel(
                ResourceKey.create(Registries.DIMENSION, parsed));
        if (lecternLevel == null) return ItemStack.EMPTY;

        net.minecraft.world.level.block.entity.BlockEntity be =
                lecternLevel.getBlockEntity(instance.getLecternPos());
        if (!(be instanceof com.piranport.dungeon.block.DungeonLecternBlockEntity lectern)) {
            return ItemStack.EMPTY;
        }
        ItemStack key = lectern.getKeyStack();
        if (key.getItem() instanceof DungeonKeyItem) return key;
        return ItemStack.EMPTY;
    }

    // ===== Event Listeners =====

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new com.piranport.dungeon.data.DungeonDataLoader());
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isInDungeon(player)) return;

        // 整合版 §3.3：不死图腾按原版逻辑在受致命伤害时结算——持有图腾时不取消事件，让原版 totem 流程触发
        if (hasTotemOfUndying(player)) {
            return;
        }

        // 取消死亡，保持库存（无论世界规则如何），加 40 tick 无敌帧
        event.setCanceled(true);
        player.setHealth(player.getMaxHealth());
        player.invulnerableTime = 40; // 2 seconds of damage immunity frames
        // 仅清除 HARMFUL 类别效果，保留装填加速/规避加成/食物 buff
        player.clearFire();
        java.util.List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> harmful =
                new java.util.ArrayList<>();
        for (var inst : player.getActiveEffects()) {
            net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effectHolder = inst.getEffect();
            if (effectHolder != null && effectHolder.value() != null
                    && effectHolder.value().getCategory()
                    == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
                harmful.add(effectHolder);
            }
        }
        for (var h : harmful) player.removeEffect(h);

        // 整合版 §3.3：死亡回门口（讲台）—— 钥匙权威源在讲台 BE（整合版 §2.2），
        // 通过 player → ACTIVE instance 反查讲台位置，再从 BE 读钥匙。
        DungeonInstanceManager mgr = DungeonInstanceManager.get((ServerLevel) player.level());
        DungeonInstance instance = findActiveInstanceForPlayer(mgr, player.getUUID());
        if (instance != null) {
            ItemStack key = readKeyFromLectern(instance, player.server);
            if (key.getItem() instanceof DungeonKeyItem) {
                UUID instanceId = DungeonKeyItem.getInstanceId(key);
                if (instanceId != null && mgr.getInstance(instanceId) != null) {
                    teleportToLectern(player, mgr.getInstance(instanceId));
                }
            }
        }

        // Notify client to open revive screen
        PacketDistributor.sendToPlayer(player, new PlayerDiedInDungeonPayload());
    }

    /**
     * 整合版 §3.3：不死图腾是减少死亡的设计，不是死亡后的处理。
     * 受致命伤害时若持有图腾，按原版逻辑结算，此时并未实际死亡。
     */
    private static boolean hasTotemOfUndying(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 整合版 §3.1：联机大厅与队长机制已作废（副本/10），不再清理 lobby / 转让钥匙。
        // 仅保留"实例为空则 SUSPENDED"检查（让原版区块卸载机制能正常卸载）。
        DungeonInstanceManager mgr = DungeonInstanceManager.get(player.server.overworld());
        UUID leavingId = player.getUUID();
        for (DungeonInstance inst : mgr.getAllInstances()) {
            if (!inst.getPlayerUuids().contains(leavingId)) continue;
            checkAndSuspendIfEmpty(player.server, inst);
        }
    }

    // ===== Node Entry Logic — delegated to DungeonNodeRouter =====

    /** Called when a player selects a node to enter. */
    public static void enterNode(ServerLevel level, DungeonInstance instance,
                                   NodeData node, StageData stage,
                                   ServerPlayer player, ItemStack keyStack) {
        DungeonNodeRouter.enterNode(level, instance, node, stage, player, keyStack);
    }

    // (Battle / Resource / Cost / Scripted handlers live in DungeonNodeRouter.)

    // ===== Portal Completion =====

    /**
     * Called when all players have entered the portal after clearing a node.
     */
    public static void onPortalComplete(ServerLevel dungeonLevel, DungeonInstance instance,
                                         String nodeId) {
        StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        if (stage == null) return;

        NodeData node = stage.nodes().get(nodeId);
        if (node == null) return;

        // Spawn loot ships for killed enemies (simplified: spawn one at portal location)
        // In a full implementation, this would track each killed enemy

        // Check if this was a boss node and all boss nodes are cleared
        if (node.type() == NodeData.NodeType.BOSS) {
            boolean allBossesCleared = stage.bossNodes().stream()
                    .allMatch(bossNode -> instance.getClearedNodes().contains(bossNode));

            if (allBossesCleared) {
                // Dungeon complete!
                completeDungeon(dungeonLevel, instance, stage);
                return;
            }
        }

        // Return players to the lectern to select next node via book
        teleportAllPlayersToLectern(dungeonLevel.getServer(), instance);
    }

    private static void completeDungeon(ServerLevel dungeonLevel, DungeonInstance instance,
                                         StageData stage) {
        MinecraftServer server = dungeonLevel.getServer();
        DungeonInstanceManager mgr = DungeonInstanceManager.get(dungeonLevel);
        DungeonSavedData savedData = DungeonSavedData.get(dungeonLevel);
        DungeonLeaderboard leaderboard = DungeonLeaderboard.get(dungeonLevel);

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - instance.getStartTimeMillis();
        mgr.completeInstance(instance.getInstanceId());

        // Process each player
        for (UUID playerUuid : instance.getPlayerUuids()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player == null) continue;

            boolean isFirstClear = !savedData.hasFirstCleared(stage.stageId(), playerUuid);

            List<String> rewardNames = new ArrayList<>();
            if (isFirstClear) {
                savedData.markFirstCleared(stage.stageId(), playerUuid);
                for (NodeData.RewardEntry reward : stage.firstClearRewards()) {
                    RewardDispatcher.give(player, reward, rewardNames);
                }
            }

            // Submit to leaderboard
            leaderboard.submit(stage.stageId(), playerUuid,
                    player.getGameProfile().getName(), elapsed);

            // Teleport back
            teleportToLectern(player, instance);

            // Send result screen
            PacketDistributor.sendToPlayer(player,
                    new DungeonResultPayload(stage.displayName(), elapsed,
                            isFirstClear, rewardNames));
        }

        // Cleanup instance
        NodeBattleField.cleanupRegion(dungeonLevel, instance);
        mgr.cleanupInstance(instance.getInstanceId());
        DungeonScriptManager.get(server).remove(instance.getInstanceId());
    }

    // ===== Utility Methods =====

    public static void teleportToLectern(ServerPlayer player, DungeonInstance instance) {
        BlockPos lecternPos = instance.getLecternPos();
        if (lecternPos == null) {
            // 整合版 §3.3：决策要求"重生到进入副本时的门口"——若实例未记录讲台位置
            // （极异常路径：旧存档或被外部清空），拒绝传送并报错，避免错误兜底到主世界 spawn
            // 导致玩家错误地认为副本丢失。改用 keepPosition+log，让玩家报告。
            PiranPort.LOGGER.error("Instance {} has no lecternPos; refusing teleport-to-lectern for {}",
                    instance.getInstanceId(), player.getName().getString());
            return;
        }

        String dimKey = instance.getLecternDimension();
        ServerLevel targetLevel = null;
        if (dimKey != null) {
            ResourceLocation parsed = ResourceLocation.tryParse(dimKey);
            if (parsed != null) {
                ResourceKey<Level> targetDim = ResourceKey.create(Registries.DIMENSION, parsed);
                targetLevel = player.server.getLevel(targetDim);
            }
            if (targetLevel == null) {
                PiranPort.LOGGER.warn(
                        "Could not resolve lectern dimension '{}' for instance {} — falling back to overworld",
                        dimKey, instance.getInstanceId());
            }
        }
        if (targetLevel == null) {
            targetLevel = player.server.overworld();
        }

        player.teleportTo(targetLevel,
                lecternPos.getX() + 0.5, lecternPos.getY() + 1, lecternPos.getZ() + 0.5,
                player.getYRot(), player.getXRot());
    }

    public static void teleportAllPlayersToLectern(MinecraftServer server,
                                                     DungeonInstance instance) {
        for (UUID playerUuid : instance.getPlayerUuids()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player != null && isInDungeon(player)) {
                teleportToLectern(player, instance);
            }
        }
    }

    public static void checkAndSuspendIfEmpty(MinecraftServer server,
                                                DungeonInstance instance) {
        ServerLevel dungeonLevel = getDungeonLevel(server);
        if (dungeonLevel == null) return;

        boolean anyOnline = false;
        for (UUID playerUuid : instance.getPlayerUuids()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player != null && isInDungeon(player)) {
                anyOnline = true;
                break;
            }
        }

        if (!anyOnline) {
            DungeonInstanceManager mgr = DungeonInstanceManager.get(dungeonLevel);
            mgr.suspendInstance(instance.getInstanceId());
        }
    }

    // ===== Boundary Protection (整合版 §2.1 / 副本/01) =====

    /**
     * 副本/01 §2.1：每个实例分配 1024×1024 子区域（实际 512×512 居中）。
     * 若玩家（因鞘翅/坐骑/推进器）跨过边界进入相邻实例子区域或走出可玩区，
     * 每 tick 拉回当前实例的 512×512 可玩区最近点。避免出现"误入他人副本"的体验事故。
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isInDungeon(player)) return;

        DungeonInstanceManager mgr = DungeonInstanceManager.get((ServerLevel) player.level());
        DungeonInstance instance = findActiveInstanceForPlayer(mgr, player.getUUID());
        if (instance == null) return;

        BlockPos pos = player.blockPosition();
        if (instance.isInsideUsableArea(pos)) return;

        BlockPos clamped = instance.clampToUsableArea(pos);
        // 仅修 X/Z 坐标，保留原 Y（防止从空中拉回水里）
        player.teleportTo(player.server.getLevel(player.level().dimension()),
                clamped.getX() + 0.5, player.getY(), clamped.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        PiranPort.LOGGER.debug("Boundary-clamped player {} from {} to {} in instance {}",
                player.getName().getString(), pos, clamped, instance.getInstanceId());
    }

    // ===== Checkpoint (整合版 §3.2) =====

    /**
     * 整合版 §3.2：玩家踩到记录点方块 → 标记玩家最新 checkpoint + S2C 反馈（光柱/标题/音效）。
     * 解锁条件由 {@link com.piranport.dungeon.data.CheckpointData#isUnlocked} 判断。
     */
    public static void onCheckpointReached(net.minecraft.server.level.ServerPlayer player,
                                              BlockPos pos) {
        DungeonInstanceManager mgr = DungeonInstanceManager.get((ServerLevel) player.level());
        UUID playerUuid = player.getUUID();
        DungeonInstance instance = null;
        for (DungeonInstance inst : mgr.getAllInstances()) {
            if (inst.getPlayerUuids().contains(playerUuid)) {
                instance = inst;
                break;
            }
        }
        if (instance == null) return;
        if (!isInDungeon(player)) return;

        com.piranport.dungeon.data.StageData stage =
                com.piranport.dungeon.data.DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        if (stage == null) return;

        com.piranport.dungeon.data.CheckpointData matched = null;
        for (com.piranport.dungeon.data.CheckpointData cp : stage.checkpoints()) {
            if (cp.posX() == pos.getX() && cp.posY() == pos.getY() && cp.posZ() == pos.getZ()) {
                matched = cp;
                break;
            }
        }
        if (matched == null) return;

        boolean nodeCleared = instance.getClearedNodes().contains(matched.nodeId());
        if (!matched.isUnlocked(nodeCleared)) {
            return;
        }

        String previousCpId = instance.getLatestCheckpointFor(playerUuid);
        if (matched.id().equals(previousCpId)) {
            return;
        }
        instance.markCheckpointReached(playerUuid, matched.id());
        mgr.setDirty();

        PacketDistributor.sendToPlayer(player,
                new com.piranport.dungeon.network.CheckpointReachedPayload(
                        stage.displayName(), matched.id()));
        PiranPort.LOGGER.info("Player {} reached checkpoint '{}' in stage {}",
                player.getName().getString(), matched.id(), instance.getStageId());
    }
}
