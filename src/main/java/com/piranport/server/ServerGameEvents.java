package com.piranport.server;

import com.piranport.PiranPort;
import com.piranport.aviation.AircraftIndex;
import com.piranport.aviation.FireControlManager;
import com.piranport.aviation.ReconManager;
import com.piranport.network.FireControlSyncPayload;
import com.piranport.combat.SalvoManager;
import com.piranport.combat.TorpedoGuidanceManager;
import com.piranport.combat.TransformationManager;
import com.piranport.config.ModCommonConfig;
import com.piranport.debug.PiranPortCommands;
import com.piranport.debug.PiranPortDebug;
import com.piranport.testtools.PiranPortTestTools;
import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.event.DungeonEventHandler;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.script.DungeonScriptManager;
import com.piranport.handler.PlayerTickHandler;
import com.piranport.npc.ai.FleetGroupManager;
import com.piranport.registry.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class ServerGameEvents {

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living && !living.level().isClientSide()) {
            com.piranport.terminal.TerminalEntityAttributeOverrides.apply(living);
        }
    }

    /**
     * 将武器类物品拾取定向到主背包（9-35格）而非快捷栏。
     * 主背包满时回退原版行为。
     */
    @SubscribeEvent
    public static void onWeaponPickup(ItemEntityPickupEvent.Pre event) {
        // 保持原版拾取规则：武器不再被重定向到主背包。
    }

    /** 每 tick 驱动地牢脚本 */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Dead or unloaded entities must disappear from both server locks and the client HUD.
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (FireControlManager.removeDeadTargets(player.getUUID(), targetUuid -> {
                net.minecraft.world.entity.Entity target = player.serverLevel().getEntity(targetUuid);
                return target == null || target.isRemoved() || !target.isAlive();
            })) {
                PacketDistributor.sendToPlayer(player, new FireControlSyncPayload(
                        FireControlManager.getTargets(player.getUUID())));
            }
        }

        ServerLevel dungeonLevel = event.getServer().getLevel(
                DungeonEventHandler.DUNGEON_DIMENSION);
        if (dungeonLevel != null) {
            com.piranport.dungeon.instance.TerrainEntryQueue.get(event.getServer()).tick(event.getServer());
            DungeonScriptManager.get(event.getServer()).tickAll(dungeonLevel);
            for (DungeonInstance instance : DungeonInstanceManager.get(dungeonLevel).getAllInstances()) {
                // 关卡总用时与服务端 tick 同源：暂停（无 tick）或玩家离开副本（SUSPENDED）都不推进，
                // 与节点计时/脚本计时器同一口径。客户端不再自行读墙钟，只显示服务端同步下来的值。
                if (instance.getState() == DungeonInstance.State.ACTIVE) instance.tickElapsed();
                com.piranport.dungeon.event.DungeonSettlementService.tickActiveNode(dungeonLevel, instance);
                com.piranport.dungeon.VictoryEvaluator.tick(dungeonLevel, instance);
            }
            // HUD 计时每 5 tick（0.25s）同步一次，足够 mm:ss 显示且暂停时自然停表
            if (event.getServer().getTickCount() % 5 == 0) {
                syncDungeonHudTimers(event.getServer(), dungeonLevel);
                com.piranport.dungeon.event.DungeonBossBroadcast.tick(dungeonLevel);
            }
            // 记录点信标光柱（粒子柱，只发给踩过该记录点的玩家）
            if (event.getServer().getTickCount() % 10 == 0) {
                com.piranport.dungeon.event.DungeonCheckpointBeacon.tick(event.getServer(), dungeonLevel);
            }
        }

        // 舰队编队清理（每10分钟）
        if (event.getServer().getTickCount() % 12000 == 0) {
            ServerLevel overworld = event.getServer().overworld();
            FleetGroupManager.get(overworld).cleanup(event.getServer());
        }

        // 整合版 §3.4：副本永不自动删除（删除 SUSPENDED 自动清理）；sweepLeaks 仅用于提升 pendingFreedIndices
        if (event.getServer().getTickCount() % 1200 == 0) {
            DungeonInstanceManager.get(event.getServer().overworld()).sweepLeaks();
        }

        // 离线玩家缓存清理（每小时）
        if (event.getServer().getTickCount() % 72000 == 0) {
            PlayerTickHandler.cleanupOfflinePlayers(event.getServer());
        }
    }

    /**
     * 把实例的累计用时推给真正在副本区域里的玩家。
     *
     * <p>HUD 层原本每帧用 {@code System.currentTimeMillis()} 减开始时间自己算，
     * 于是暂停游戏后墙钟照走、计时器照跑。改为服务端权威值下发后，客户端只负责显示，
     * 暂停时没有 tick 也就没有新包，表自然停住。</p>
     *
     * <p>只发给活着且非旁观、且当前确实在该实例区域内的玩家：死亡界面/复活界面期间
     * 不能把已清空的 HUD 重新点亮。</p>
     */
    private static void syncDungeonHudTimers(net.minecraft.server.MinecraftServer server,
                                             ServerLevel dungeonLevel) {
        DungeonInstanceManager mgr = DungeonInstanceManager.get(dungeonLevel);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            DungeonInstance instance = mgr.getInstanceForPlayer(player);
            if (instance == null) continue;
            com.piranport.dungeon.data.StageData stage =
                    com.piranport.dungeon.data.DungeonRegistry.INSTANCE.getStage(instance.getStageId());
            PacketDistributor.sendToPlayer(player, new com.piranport.dungeon.network.DungeonStatePayload(
                    stage == null ? instance.getStageId() : stage.displayName(),
                    instance.getCurrentNode() == null ? "" : instance.getCurrentNode(),
                    instance.getElapsedMillis()));
        }
    }

    /** 地牢脚本标记的生物死亡时通知脚本管理器 */
    @SubscribeEvent
    public static void onDungeonMobDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;
        LivingEntity entity = event.getEntity();

        for (String tag : entity.getTags()) {
            if (tag.startsWith("dungeon_instance_")) {
                try {
                    UUID instanceId = UUID.fromString(
                            tag.substring("dungeon_instance_".length()));
                    DungeonInstance instance = DungeonInstanceManager.get(sl).getInstance(instanceId);
                    if (instance == null || instance.getState() != DungeonInstance.State.ACTIVE) return;
                    for (String nodeTag : entity.getTags()) {
                        if (nodeTag.startsWith("dungeon_node_")) {
                            com.piranport.dungeon.event.DungeonSettlementService.recordKill(sl, instance,
                                    nodeTag.substring("dungeon_node_".length()), entity.getUUID());
                        }
                    }
                    if (entity.getTags().contains("dungeon_script")) {
                        DungeonScriptManager.get(sl.getServer()).onEntityDeath(instanceId, entity);
                    }
                } catch (IllegalArgumentException ignored) {}
                break;
            }
        }
    }

    /** 地牢旗舰死亡时检查同节点所有旗舰，全灭则生成传送门 */
    @SubscribeEvent
    public static void onDungeonFlagshipDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;
        LivingEntity entity = event.getEntity();

        UUID instanceId = null;
        String nodeId = null;
        for (String tag : entity.getTags()) {
            if (tag.startsWith("dungeon_instance_")) {
                try {
                    instanceId = UUID.fromString(tag.substring("dungeon_instance_".length()));
                } catch (IllegalArgumentException ignored) {}
            } else if (tag.startsWith("dungeon_node_")) {
                nodeId = tag.substring("dungeon_node_".length());
            }
        }
        if (instanceId == null || nodeId == null) return;

        DungeonInstanceManager mgr = DungeonInstanceManager.get(sl);
        DungeonInstance instance = mgr.getInstance(instanceId);
        if (instance == null || instance.getState() != DungeonInstance.State.ACTIVE
                || instance.getClearedNodes().contains(nodeId)) return;
        com.piranport.dungeon.event.DungeonSettlementService.recordKill(sl, instance, nodeId, entity.getUUID());
        var objectives = com.piranport.dungeon.saved.DungeonObjectiveData.get(sl);
        objectives.died(instanceId, nodeId, entity.getUUID());

        if (DungeonScriptManager.get(sl.getServer()).getScript(instanceId) != null) return;

        var stage = com.piranport.dungeon.data.DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        if (stage == null) return;
        boolean allEnemies = stage.victoryConditions().contains(com.piranport.dungeon.data.VictoryCondition.KILL_ALL)
                || stage.victoryConditions().contains(com.piranport.dungeon.data.VictoryCondition.CAPTURE_FLAG);
        var node = stage.nodes().get(nodeId);
        var enemies = node == null || node.enemies() == null ? null
                : com.piranport.dungeon.data.DungeonRegistry.INSTANCE.getEnemySet(node.enemies());
        allEnemies |= enemies != null && enemies.flagship() == null;
        boolean anyFlagshipAlive = !objectives.defeated(instanceId, nodeId, allEnemies);

        if (!anyFlagshipAlive && node != null) {
            // 《副本/00》多人波次：快照总波数未打完 → 刷下一波，不开门
            int wave = instance.getNodeWave(nodeId);
            int total = instance.getNodeWaveTotal(nodeId);
            if (com.piranport.dungeon.instance.NodeBattleField.trySpawnNextWave(sl, instance, node)) {
                for (ServerPlayer player : mgr.getPresentPlayers(instance, sl.getServer())) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                            "dungeon.piranport.next_wave", wave + 1, total), true);
                }
                return;
            }
            // 首通参与判定：Boss 击杀瞬间位于 Boss 节点 128×128 范围内的玩家
            if (node.type() == com.piranport.dungeon.data.NodeData.NodeType.BOSS) {
                com.piranport.dungeon.event.DungeonEventHandler.snapshotBossParticipants(
                        sl.getServer(), instance, nodeId);
            }
        }

        if (!anyFlagshipAlive) {
            BlockPos portalPos = entity.blockPosition();
            com.piranport.dungeon.block.PortalStructureHelper.buildPortalStructure(
                    sl, portalPos.below(), instanceId, nodeId);

            for (UUID playerUuid : instance.getPlayerUuids()) {
                ServerPlayer player = sl.getServer().getPlayerList().getPlayer(playerUuid);
                if (player != null && mgr.getInstanceForPlayer(player) == instance) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "dungeon.piranport.node_cleared"), true);
                }
            }
        }
    }

    /**
     * 经验提升 Buff：按策划 §7.7 表，按等级缩放。
     * <ul>
     *   <li>等级 1 (amplifier=0) → ×1.2</li>
     *   <li>等级 2 (amplifier=1) → ×1.4</li>
     *   <li>等级 3 (amplifier=2) → ×1.6</li>
     * </ul>
     * 实际倍率 = 1.2 + amplifier * 0.2（amplifier 上限 clamp 到 2）。
     */
    @SubscribeEvent
    public static void onXpDrop(LivingExperienceDropEvent event) {
        Player attacker = event.getAttackingPlayer();
        if (attacker != null && attacker.hasEffect(ModMobEffects.EXPERIENCE_BOOST)) {
            int amp = attacker.getEffect(ModMobEffects.EXPERIENCE_BOOST).getAmplifier();
            double multiplier = com.piranport.effect.CombatEffectRules.experienceMultiplier(amp);
            int original = event.getDroppedExperience();
            event.setDroppedExperience((int) (original * multiplier));
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        PiranPortCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        // 清理顺序说明：
        // 1. 先关闭所有调试会话（写摘要、重命名归档文件）
        // 2. 强制关闭测试模式（与调试隔离的独立工具）
        // 3. 清理战斗系统状态（火控、侦察、鱼雷制导），这些依赖玩家和实体
        // 4. 再清理飞机索引（依赖玩家UUID）
        // 5. 最后清理玩家Tick缓存（独立数据，无依赖）
        PiranPortDebug.closeAll(PiranPortDebug.CloseReason.SERVER_STOP);
        PiranPortTestTools.closeAll();
        FireControlManager.clearAll();
        ReconManager.clearAll();
        TorpedoGuidanceManager.clearAll();
        SalvoManager.clearAll();
        AircraftIndex.clearAll();
        PlayerTickHandler.clearCaches();
    }

    /** 玩家登出时关闭其调试会话，避免日志文件泄漏 */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
            PiranPortDebug.closePlayer(sp.getUUID(), PiranPortDebug.CloseReason.LOGOUT);
            // 测试模式水印：玩家登出时若是测试者，强制关闭
            PiranPortTestTools.onPlayerLogout(sp.getUUID());
        }
    }

    /** 每 200 tick 检查一次会话超时（30 分钟） */
    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        if ((event.getServer().getTickCount() & 0xFF) != 0) return;
        PiranPortDebug.evictExpired();
    }
}
