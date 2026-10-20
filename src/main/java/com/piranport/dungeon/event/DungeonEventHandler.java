package com.piranport.dungeon.event;

import com.piranport.PiranPort;
import com.piranport.dungeon.VictoryEvaluator;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.instance.NodeBattleField;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.network.DungeonResultPayload;
import com.piranport.dungeon.saved.DungeonSavedData;
import com.piranport.dungeon.script.DungeonScriptManager;
import com.piranport.registry.ModItems;
import com.piranport.advancement.ModAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
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
import net.neoforged.neoforge.event.server.ServerStartedEvent;
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
     * 从副本实例对应的讲台 BE 读取钥匙 ItemStack。
     * 整合版 §2.2：钥匙权威源是讲台 BE（讲台在 instance.lecternDimension 维度的 instance.lecternPos 位置），
     * 不再扫玩家背包。读到的钥匙其 DUNGEON_INSTANCE_ID 应等于 instance.getInstanceId()。
     *
     * @return BE 中的钥匙；若讲台缺失或 BE 中无钥匙，返回 ItemStack.EMPTY
     */
    static ItemStack readKeyFromLectern(DungeonInstance instance, MinecraftServer server) {
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

    static void markLecternChanged(DungeonInstance instance, MinecraftServer server) {
        if (instance.getLecternPos() == null || instance.getLecternDimension() == null) return;
        ResourceLocation id = ResourceLocation.tryParse(instance.getLecternDimension());
        if (id == null) return;
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        if (level != null && level.getBlockEntity(instance.getLecternPos()) instanceof
                com.piranport.dungeon.block.DungeonLecternBlockEntity lectern) lectern.setChanged();
    }

    // ===== Event Listeners =====

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new com.piranport.dungeon.data.DungeonDataLoader());
    }

    /**
     * 副本维度由数据包（{@code data/piranport/dimension/dungeon.json}）定义，随服务器启动一次性构造/加载，
     * 不是每个实例各建一个维度。这里打一行确认维度可用，避免把「实例建造」误当成「构造维度」。
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (getDungeonLevel(event.getServer()) == null) {
            PiranPort.LOGGER.warn("[副本生成] 维度 {} 未加载：副本实例将无法建造，请检查数据包 dimension 定义",
                    DUNGEON_DIMENSION.location());
        } else {
            PiranPort.LOGGER.info("[副本生成] 维度 {} 已构造完成（服务器启动随数据包加载，非每实例新建）",
                    DUNGEON_DIMENSION.location());
        }
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
        DungeonInstance instance = mgr.getInstanceForPlayer(player);
        if (instance != null) {
            VictoryEvaluator.onFlagPlayerDied((ServerLevel) player.level(), instance, player);
            // 讲台是位置坐标，不依赖钥匙在场：第三方中途拔钥匙时玩家仍应回门口。
            // 若钥匙/讲台已毁，读不到钥匙仅意味着副本会话无法继续，不阻塞传送。
            ItemStack key = readKeyFromLectern(instance, player.server);
            DungeonInstance target = instance;
            if (key.getItem() instanceof DungeonKeyItem) {
                UUID instanceId = DungeonKeyItem.getInstanceId(key);
                if (instanceId != null && mgr.getInstance(instanceId) != null) {
                    target = mgr.getInstance(instanceId);
                }
            }

            // 2026-10-18：删除复活确认界面。与不死图腾的原版结算一致——背包里有图腾
            // 就直接消耗并传送回当前节点，不再弹确认框；没有图腾才回讲台。
            String node = target.getCurrentNode();
            int totemSlot = findTotemSlot(player);
            ServerLevel dungeonLevel = getDungeonLevel(player.server);
            boolean revived = false;
            if (totemSlot >= 0 && node != null && dungeonLevel != null) {
                player.getInventory().getItem(totemSlot).shrink(1);
                BlockPos spawn = target.getNodeSpawnPos(node);
                player.teleportTo(dungeonLevel,
                        spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                        player.getYRot(), player.getXRot());
                revived = true;
            }

            if (!revived) {
                teleportToLectern(player, target);
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        totemSlot < 0
                                ? "dungeon.piranport.revive_no_totem"
                                : "dungeon.piranport.revive_unavailable"));
            }
        } else {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "dungeon.piranport.revive_unavailable"));
        }
    }

    /** 背包（含副手）中第一个不死图腾的槽位；没有时返回 -1。 */
    private static int findTotemSlot(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(Items.TOTEM_OF_UNDYING)) return i;
        }
        return -1;
    }

    /**
     * 整合版 §3.3：不死图腾是减少死亡的设计，不是死亡后的处理。
     * 受致命伤害时若持有图腾，按原版逻辑结算，此时并未实际死亡。
     */
    private static boolean hasTotemOfUndying(ServerPlayer player) {
        // 原版 LivingEntity.checkTotemDeathProtection 只检查主手和副手；
        // 背包里的图腾不会触发原版保护——若这里误判为持有，事件不取消，
        // 玩家会真死并在主世界重生，违背「死亡回门口」设计。
        return player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)
                || player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DungeonInstanceManager mgr = DungeonInstanceManager.get(player.server.overworld());
        mgr.handlePlayerDisconnect(player.server, player.getUUID());
    }

    /** 重连后按当前区域恢复，保留已保存的脚本阶段。 */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DungeonInstanceManager.get(player.serverLevel()).refreshPlayerPresence(player.server);
    }

    /** 包含其他模组或指令发起的跨维度退出，统一交由实例管理器判断是否全员离开。 */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DungeonInstanceManager.get(player.serverLevel()).refreshPlayerPresence(player.server);
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

    /** 真实胜利判定的唯一节点完成入口；奖励和结算界面在此立即处理。 */
    public static void onNodeCompleted(ServerLevel dungeonLevel, DungeonInstance instance,
                                       String nodeId) {
        StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        if (stage == null) return;

        NodeData node = stage.nodes().get(nodeId);
        if (node == null) return;

        DungeonInstanceManager manager = DungeonInstanceManager.get(dungeonLevel);
        if (node.type() == NodeData.NodeType.BOSS && !instance.getClearedNodes().contains(nodeId)
                && !bossSnapshotTakenFor(instance, nodeId)) {
            // 脚本 Boss / 胜利目标判定等不经 onDungeonFlagshipDeath 的路径在此兜底快照
            snapshotBossParticipants(dungeonLevel.getServer(), instance, nodeId);
        }
        BOSS_SNAPSHOT_NODE.remove(instance.getInstanceId());
        if (!manager.markNodeCleared(instance.getInstanceId(), nodeId,
                readKeyFromLectern(instance, dungeonLevel.getServer()))) return;
        com.piranport.dungeon.escort.EscortRuntime.releaseTransport(dungeonLevel, instance, nodeId);
        markLecternChanged(instance, dungeonLevel.getServer());
        boolean completes;
        if (stage.bossNodes().isEmpty()) {
            // 无 Boss 的活动关：终点节点（无出边）全部清完才算通关。分歧关允许跳过某条支路，
            // 若要求所有战斗节点都清，玩家走 A→C 绕过 B 时将永远无法通关。
            completes = stage.nodes().keySet().stream()
                    .filter(id -> stage.edges().stream().noneMatch(e -> e.from().equals(id)))
                    .allMatch(instance.getClearedNodes()::contains);
        } else {
            completes = node.type() == NodeData.NodeType.BOSS
                    && stage.bossNodes().stream().allMatch(instance.getClearedNodes()::contains);
        }
        DungeonSettlementService.settleNode(dungeonLevel, instance, stage, node, completes);
        if (completes) completeDungeon(dungeonLevel, instance, stage);
    }

    /** 传送门只负责离开/推进；它不能把节点标记完成或再次结算。 */
    public static void onPortalComplete(ServerLevel dungeonLevel, DungeonInstance instance,
                                         String nodeId) {
        if (instance.getClearedNodes().contains(nodeId)) {
            teleportAllPlayersToLectern(dungeonLevel.getServer(), instance);
        }
    }

    /** 已完成节点的出口按玩家独立使用，不把其他成员从战场强制传走。 */
    public static void onPortalEntered(ServerPlayer player, DungeonInstance instance, String nodeId) {
        onPortalEntered(player, instance, nodeId, "");
    }

    /** 玩家走近已清分歧节点的门群：定格本群亮灭（首次抵达时掷概率，之后复用快照）。 */
    public static void onPortalApproached(ServerPlayer player, DungeonInstance instance, String nodeId) {
        if (!instance.getClearedNodes().contains(nodeId)
                || DungeonInstanceManager.get(player.serverLevel()).getInstanceForPlayer(player) != instance) {
            return;
        }
        StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        NodeData node = stage == null ? null : stage.nodes().get(nodeId);
        if (node != null) DungeonBranchRouter.reachGroup(player, instance, node);
    }

    /**
     * @param targetNodeId 这扇门明确通往的节点；空串表示旧存档的单门自动选路。
     */
    public static void onPortalEntered(ServerPlayer player, DungeonInstance instance, String nodeId,
                                       String targetNodeId) {
        if (!instance.getClearedNodes().contains(nodeId)
                || DungeonInstanceManager.get(player.serverLevel()).getInstanceForPlayer(player) != instance) {
            return;
        }
        // 《副本/12》：非 Boss 终点不退本，出口直接送往下一条节点（有分支时走带路判定）。
        // 只有当前节点没有后继、实例已通关或无法推进时，才按原逻辑送回讲台。
        StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        NodeData node = stage == null ? null : stage.nodes().get(nodeId);
        if (node != null && DungeonProgressionRouter.tryAdvance(
                player, instance, stage, node, targetNodeId)) {
            return;
        }
        teleportToLectern(player, instance);
    }

    /** 本 tick 已在击杀瞬间快照过的 Boss 节点（instanceId → nodeId），避免 onNodeCompleted 重拍。 */
    private static final java.util.Map<UUID, String> BOSS_SNAPSHOT_NODE = new java.util.HashMap<>();

    private static boolean bossSnapshotTakenFor(DungeonInstance instance, String nodeId) {
        return nodeId.equals(BOSS_SNAPSHOT_NODE.get(instance.getInstanceId()));
    }

    /**
     * 《副本/00》首通参与判定：Boss 击杀瞬间位于 Boss 节点 128×128 战场范围内（副本维度、存活、非旁观）的参与者。
     */
    public static void snapshotBossParticipants(MinecraftServer server, DungeonInstance instance, String nodeId) {
        BlockPos center = instance.getNodeSpawnPos(nodeId);
        List<UUID> inside = new ArrayList<>();
        for (UUID uuid : instance.getPlayerUuids()) {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p == null || !p.isAlive() || p.isSpectator() || !isInDungeon(p)) continue;
            if (com.piranport.dungeon.instance.DungeonScaling.isInsideNodeTile(p.getX(), p.getZ(),
                    center.getX() + 0.5, center.getZ() + 0.5, com.piranport.dungeon.DungeonConstants.NODE_AREA_SIZE)) {
                inside.add(uuid);
            }
        }
        instance.snapshotBossParticipants(inside);
        BOSS_SNAPSHOT_NODE.put(instance.getInstanceId(), nodeId);
        DungeonInstanceManager.get(server.overworld()).setDirty();
    }

    private static void completeDungeon(ServerLevel dungeonLevel, DungeonInstance instance,
                                         StageData stage) {
        MinecraftServer server = dungeonLevel.getServer();
        DungeonInstanceManager mgr = DungeonInstanceManager.get(dungeonLevel);
        if (!mgr.completeInstance(instance.getInstanceId())) return;
        // 结算保留现场供玩家自行离开；脚本调度器看到 COMPLETED 后自行移除，
        // 避免脚本正在遍历时回调删除当前 Map 条目。

        // 奖励与结算页已在 settleNode(completes=true) 发放；
        // 这里只负责回城与清理，避免再发一页空奖励把刚发的列表盖掉。
        for (UUID playerUuid : instance.getPlayerUuids()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player == null) continue;
            teleportToLectern(player, instance);
        }

        // Cleanup instance
        NodeBattleField.cleanupRegion(dungeonLevel, instance);
        mgr.cleanupInstance(instance.getInstanceId());
        DungeonScriptManager.get(server).remove(instance.getInstanceId());
    }

    // ===== Utility Methods =====

    /**
     * 回进本书台面前；实例没有书台坐标时回主世界出生点（《副本/00》§3.3）。
     * 两条路径都先清客户端副本 HUD，跨维度不会可靠触发客户端状态清理。
     */
    public static boolean teleportToLectern(ServerPlayer player, DungeonInstance instance) {
        BlockPos lecternPos = instance.getLecternPos();
        ServerLevel targetLevel;
        double x;
        double y;
        double z;
        if (lecternPos == null) {
            targetLevel = player.server.overworld();
            BlockPos spawn = targetLevel.getSharedSpawnPos();
            x = spawn.getX() + 0.5;
            y = spawn.getY();
            z = spawn.getZ() + 0.5;
        } else {
            targetLevel = resolveLecternLevel(player, instance);
            x = lecternPos.getX() + 0.5;
            y = lecternPos.getY() + 1;
            z = lecternPos.getZ() + 0.5;
        }

        PacketDistributor.sendToPlayer(player, new com.piranport.dungeon.network.DungeonStatePayload("", "", 0L, "", ""));
        player.teleportTo(targetLevel, x, y, z, player.getYRot(), player.getXRot());
        return true;
    }

    private static ServerLevel resolveLecternLevel(ServerPlayer player, DungeonInstance instance) {
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
        return targetLevel != null ? targetLevel : player.server.overworld();
    }

    public static void teleportAllPlayersToLectern(MinecraftServer server,
                                                     DungeonInstance instance) {
        for (UUID playerUuid : instance.getPlayerUuids()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player != null && DungeonInstanceManager.get(server.overworld()).getInstanceForPlayer(player) == instance) {
                teleportToLectern(player, instance);
            }
        }
    }

    public static void checkAndSuspendIfEmpty(MinecraftServer server,
                                                DungeonInstance instance) {
        DungeonInstanceManager.get(server.overworld()).refreshPlayerPresence(server);
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
        DungeonInstance instance = mgr.getInstanceForPlayer(player);
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

        // 鞘翅低空滑出边界时，钳制落点可能是开阔海面——脚下 5 格无实体方块则改送节点平台，
        // 否则玩家会被拉回虚空/水面上方直接坠落。
        ServerLevel dungeonLevel = (ServerLevel) player.level();
        BlockPos feet = player.blockPosition();
        boolean groundBelow = false;
        for (int dy = 0; dy <= 5; dy++) {
            if (!dungeonLevel.getBlockState(feet.below(dy)).isAir()) {
                groundBelow = true;
                break;
            }
        }
        if (!groundBelow && player.getY() < com.piranport.dungeon.DungeonConstants.SEA_LEVEL) {
            String nodeId = instance.getCurrentNode();
            BlockPos spawn = nodeId != null ? instance.getNodeSpawnPos(nodeId) : null;
            if (spawn != null) {
                player.teleportTo(dungeonLevel, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                        player.getYRot(), player.getXRot());
                PiranPort.LOGGER.debug("Boundary-clamp had no ground below; relocated player {} to node spawn {}",
                        player.getName().getString(), spawn);
            }
        }
    }

    // ===== Checkpoint (整合版 §3.2) =====

    /**
     * 整合版 §3.2：玩家踩到记录点方块 → 标记玩家最新 checkpoint + S2C 反馈（标题 + 轻快音效）。
     * 解锁条件由 {@link com.piranport.dungeon.data.CheckpointData#isUnlocked} 判断。
     *
     * <p>决策要求"信标光柱 + 屏幕标题 + 轻快音效"三件套：标题 + 音效由 CheckpointReachedPayload 客户端播放，
     * 光柱由 {@link DungeonCheckpointBeacon} 每 10 tick 以 END_ROD 粒子柱下发。</p>
     */
    public static void onCheckpointReached(net.minecraft.server.level.ServerPlayer player,
                                              BlockPos pos) {
        DungeonInstanceManager mgr = DungeonInstanceManager.get((ServerLevel) player.level());
        UUID playerUuid = player.getUUID();
        DungeonInstance instance = mgr.getInstanceForPlayer(player);
        if (instance == null) return;
        if (!isInDungeon(player)) return;

        com.piranport.dungeon.data.StageData stage =
                com.piranport.dungeon.data.DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        if (stage == null) return;

        com.piranport.dungeon.data.CheckpointData matched = null;
        for (com.piranport.dungeon.data.CheckpointData cp : stage.checkpoints()) {
            if (DungeonEntryRules.checkpointPosition(instance, cp).equals(pos)) {
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

    /**
     * 潜行持物右键讲台的兜底取钥匙。
     *
     * <p>WHY：NeoForge flag1 闸门在「潜行 + 任一手非空」时跳过方块的所有交互回调，
     * 除非两手物品的 doesSneakBypassUse 都返回 true。钥匙只对拿钥匙的手放行，
     * 另一手拿任意其他物品（舰装核心等）时 useWithoutItem 永远进不去，钥匙撤不下来。
     * 该闸门在事件之后才判定，所以在服务端事件里直接补一遍 useWithoutItem 的潜行分支。
     * 主副手各触发一次事件，取消并返回 CONSUME 后第二只手不会重复执行。</p>
     */
    @SubscribeEvent
    public static void onRightClickBlockForLectern(
            net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)
                || serverPlayer.level().isClientSide()) return;
        if (!serverPlayer.isSecondaryUseActive()) return;
        BlockPos pos = event.getPos();
        if (!(serverPlayer.level().getBlockState(pos).getBlock()
                instanceof com.piranport.dungeon.block.DungeonLecternBlock)) return;
        if (!(serverPlayer.level().getBlockEntity(pos)
                instanceof com.piranport.dungeon.block.DungeonLecternBlockEntity lecternBE)) return;
        if (!lecternBE.hasKey()) {
            serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "block.piranport.dungeon_lectern.no_key_to_take"), true);
        } else {
            lecternBE.refreshStatus();
            if (!com.piranport.dungeon.block.LecternPattern.canWithdraw(lecternBE.getStatus())) {
                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "block.piranport.dungeon_lectern.key_locked"), true);
            } else {
                lecternBE.extractKeyForShiftRightClick(serverPlayer);
            }
        }
        // CONSUME：方块逻辑已处理过但不算「用了手中物品」，潜行持物不会再走物品使用。
        event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
        event.setCanceled(true);
    }

    // ===== 钥匙三源：章节通关纪念章发放 =====
    private static void giveDeployMedal(ServerPlayer player, int chapterNum) {
        if (chapterNum < 1 || chapterNum > 7) return;
        ItemStack medal = switch (chapterNum) {
            case 1 -> new ItemStack(ModItems.DEPLOY_MEDAL_CH1.get());
            case 2 -> new ItemStack(ModItems.DEPLOY_MEDAL_CH2.get());
            case 3 -> new ItemStack(ModItems.DEPLOY_MEDAL_CH3.get());
            case 4 -> new ItemStack(ModItems.DEPLOY_MEDAL_CH4.get());
            case 5 -> new ItemStack(ModItems.DEPLOY_MEDAL_CH5.get());
            case 6 -> new ItemStack(ModItems.DEPLOY_MEDAL_CH6.get());
            case 7 -> new ItemStack(ModItems.DEPLOY_MEDAL_CH7.get());
            default -> ItemStack.EMPTY;
        };
        if (!medal.isEmpty()) {
            if (!player.getInventory().add(medal)) {
                player.drop(medal, false);
            }
            PiranPort.LOGGER.info("Gave deploy medal Ch{} to {}", chapterNum, player.getName().getString());
        }
    }

    private static int parseChapterFromStageId(String stageId) {
        if (stageId == null || stageId.isEmpty()) return -1;
        if (stageId.startsWith("chapter_")) {
            try {
                return Integer.parseInt(stageId.substring("chapter_".length()));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
}
