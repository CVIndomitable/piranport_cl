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

    private record TrackedBoss(UUID bossUuid, String nodeId, String shipType) {}

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

    /** 每 5 tick 推一次铭牌；Boss 死亡/消失后推一次 visible=false 收起。 */
    public static void tick(ServerLevel dungeonLevel) {
        if (BOSSES.isEmpty()) return;
        MinecraftServer server = dungeonLevel.getServer();
        DungeonInstanceManager mgr = DungeonInstanceManager.get(dungeonLevel);
        Iterator<Map.Entry<UUID, TrackedBoss>> it = BOSSES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, TrackedBoss> entry = it.next();
            DungeonInstance instance = mgr.getInstance(entry.getKey());
            if (instance == null || instance.getState() == DungeonInstance.State.CLEANUP) {
                it.remove();
                continue;
            }
            TrackedBoss tracked = entry.getValue();
            Entity boss = dungeonLevel.getEntity(tracked.bossUuid());
            boolean nodeCleared = instance.getClearedNodes().contains(tracked.nodeId());
            // 区块卸载导致暂时取不到实体：保持客户端现状，不误收起
            if (boss == null && !nodeCleared) continue;
            boolean alive = boss instanceof LivingEntity living && living.isAlive();
            float health = alive ? Math.max(0f, ((LivingEntity) boss).getHealth()) : 0f;
            float maxHealth = alive ? Math.max(1f, ((LivingEntity) boss).getMaxHealth()) : 1f;
            int segment = alive ? Math.max(0, Math.min(4, (int) Math.ceil(health / maxHealth * 4.0f))) : 0;
            String name = alive ? boss.getDisplayName().getString() : "";
            var stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
            DungeonBossOverlayPayload payload = new DungeonBossOverlayPayload(name, tracked.shipType(),
                    stage == null ? "" : stage.chapter(), segment, health, maxHealth, alive, false);
            for (ServerPlayer player : mgr.getPresentPlayers(instance, server)) {
                PacketDistributor.sendToPlayer(player, payload);
            }
            if (!alive) it.remove();
        }
    }
}
