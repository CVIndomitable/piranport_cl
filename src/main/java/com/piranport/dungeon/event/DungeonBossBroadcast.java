package com.piranport.dungeon.event;

import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.network.DungeonBossOverlayPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 《副本/00》Boss 触发全实例通报：标题 + 音效 + 节点名，不新增 HUD；
 * Boss 生成后铭牌/分段血条（复用 {@link DungeonBossOverlayPayload}）对实例内全员同步，
 * 而不只是触发节点的玩家。
 */
public final class DungeonBossBroadcast {
    private DungeonBossBroadcast() {}

    /**
     * 单个 Boss 铭牌的追踪状态。
     *
     * <p>{@code recipients} 记录最近一次收到铭牌的玩家：实例被清理（通关后）时实例已经不存在，
     * 没法再按“实例内玩家”取接收者，必须靠这份记录把 visible=false 补发出去，否则客户端会
     * 一直挂着最后一次收到的血条。</p>
     */
    private static final class TrackedBoss {
        final UUID bossUuid;
        final String nodeId;
        final String shipType;
        final java.util.Set<UUID> recipients = new java.util.HashSet<>();
        /** 实体暂时取不到（区块卸载）的连续 tick 数，超过阈值就当作已消失收起。 */
        int staleTicks;

        TrackedBoss(UUID bossUuid, String nodeId, String shipType) {
            this.bossUuid = bossUuid;
            this.nodeId = nodeId;
            this.shipType = shipType;
        }
    }

    /** 区块卸载时最多容忍 10 秒（200 tick）取不到实体，之后强制收起避免血条永久残留。 */
    private static final int MAX_STALE_TICKS = 200;

    private static DungeonBossOverlayPayload hiddenPayload() {
        return new DungeonBossOverlayPayload("", "", "", 0, 0f, 1f, false, false);
    }

    /** 向所有看过这块铭牌的玩家补发隐藏包。实例已不存在时也能定位接收者。 */
    private static void hideFor(MinecraftServer server, TrackedBoss tracked) {
        DungeonBossOverlayPayload payload = hiddenPayload();
        for (UUID id : tracked.recipients) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) PacketDistributor.sendToPlayer(player, payload);
        }
        tracked.recipients.clear();
    }

    /** instanceId → 当前 Boss。纯内存：重启后由下一次生成重新登记，铭牌只是表现层。 */
    private static final Map<UUID, TrackedBoss> BOSSES = new HashMap<>();

    /** Boss 节点激活瞬间：对实例内所有在场玩家发标题 + 音效 + 节点名。 */
    public static void announceBossNode(MinecraftServer server, DungeonInstance instance, NodeData node) {
        var stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        String stageName = stage == null ? instance.getStageId() : stage.displayName();
        Component title = Component.translatable("dungeon.piranport.boss_alert.title");
        Component subtitle = Component.translatable("dungeon.piranport.boss_alert.subtitle",
                stageName, node.nodeId());
        DungeonInstanceManager mgr = DungeonInstanceManager.get(server.overworld());
        for (ServerPlayer player : mgr.getPresentPlayers(instance, server)) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            player.sendSystemMessage(Component.translatable("dungeon.piranport.boss_alert.chat",
                    stageName, node.nodeId()));
            player.playNotifySound(SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.0f, 1.0f);
        }
    }

    /** NodeBattleField 生成 Boss 节点敌人后登记旗舰，供铭牌同步。 */
    public static void trackBoss(DungeonInstance instance, NodeData node, List<Entity> spawned, String flagshipEntityId) {
        if (node.type() != NodeData.NodeType.BOSS) return;
        for (Entity e : spawned) {
            if (e instanceof LivingEntity && e.getTags().contains("dungeon_flagship")) {
                int colon = flagshipEntityId == null ? -1 : flagshipEntityId.lastIndexOf(':');
                String shipType = flagshipEntityId == null ? ""
                        : (colon >= 0 ? flagshipEntityId.substring(colon + 1) : flagshipEntityId).replace('_', ' ');
                BOSSES.put(instance.getInstanceId(), new TrackedBoss(e.getUUID(), node.nodeId(), shipType));
                return;
            }
        }
    }

    /** 护航节点刷出运输船后登记：血量同样以 Boss 条显示给实例内全员。 */
    public static void trackEscort(DungeonInstance instance, Entity transport, String nodeId) {
        String shipType = transport.getType().getDescription().getString();
        BOSSES.put(instance.getInstanceId(), new TrackedBoss(transport.getUUID(), nodeId, shipType));
    }

    /** 每 5 tick 推一次铭牌；Boss 死亡/消失后推一次 visible=false 收起。 */
    public static void tick(ServerLevel dungeonLevel) {
        if (BOSSES.isEmpty()) return;
        MinecraftServer server = dungeonLevel.getServer();
        DungeonInstanceManager mgr = DungeonInstanceManager.get(dungeonLevel);
        Iterator<Map.Entry<UUID, TrackedBoss>> it = BOSSES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, TrackedBoss> entry = it.next();
            DungeonInstance instance = mgr.getInstance(entry.getKey());
            TrackedBoss tracked = entry.getValue();
            if (instance == null || instance.getState() == DungeonInstance.State.CLEANUP) {
                // 实例已结束：实例对象没了，只能按接收者记录补发隐藏，否则客户端血条永久残留。
                hideFor(server, tracked);
                it.remove();
                continue;
            }
            Entity boss = dungeonLevel.getEntity(tracked.bossUuid);
            boolean nodeCleared = instance.getClearedNodes().contains(tracked.nodeId);
            if (boss == null && !nodeCleared) {
                // 区块卸载导致暂时取不到实体：先保持客户端现状，但超过阈值必须收起，
                // 否则 Boss 被移除而节点又没标记清空时，旧血条会一直挂着。
                if (++tracked.staleTicks > MAX_STALE_TICKS) {
                    hideFor(server, tracked);
                    it.remove();
                }
                continue;
            }
            tracked.staleTicks = 0;
            boolean alive = boss instanceof LivingEntity living && living.isAlive();
            float health = alive ? Math.max(0f, ((LivingEntity) boss).getHealth()) : 0f;
            float maxHealth = alive ? Math.max(1f, ((LivingEntity) boss).getMaxHealth()) : 1f;
            int segment = alive ? Math.max(0, Math.min(4, (int) Math.ceil(health / maxHealth * 4.0f))) : 0;
            String name = alive ? boss.getDisplayName().getString() : "";
            var stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
            DungeonBossOverlayPayload payload = new DungeonBossOverlayPayload(name, tracked.shipType,
                    stage == null ? "" : stage.chapter(), segment, health, maxHealth, alive, false);

            java.util.Set<UUID> present = new java.util.HashSet<>();
            for (ServerPlayer player : mgr.getPresentPlayers(instance, server)) {
                present.add(player.getUUID());
                tracked.recipients.add(player.getUUID());
                PacketDistributor.sendToPlayer(player, payload);
            }
            // 已经离开实例的玩家：补发隐藏，避免他们把铭牌带回主世界。
            if (!tracked.recipients.isEmpty()) {
                DungeonBossOverlayPayload hidden = hiddenPayload();
                for (UUID id : new java.util.ArrayList<>(tracked.recipients)) {
                    if (present.contains(id)) continue;
                    ServerPlayer player = server.getPlayerList().getPlayer(id);
                    if (player != null) PacketDistributor.sendToPlayer(player, hidden);
                    tracked.recipients.remove(id);
                }
            }
            if (!alive) {
                hideFor(server, tracked);
                it.remove();
            }
        }
    }
}
