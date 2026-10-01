package com.piranport.dungeon.event;

import com.piranport.dungeon.data.CheckpointData;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * 《副本/00》记录点信标光柱：用原版 END_ROD 粒子柱模拟信标光束，不新增贴图/方块实体。
 *
 * <p>对实例内在场玩家显示所有已解锁的记录点（导航用）；玩家自己最新激活的记录点额外叠加
 * 一圈 HAPPY_VILLAGER 粒子，区分“已激活”。只发给该玩家（sendParticles 带 player 参数），
 * 其他实例看不到。距离 > 160 格不发，控制包量。</p>
 */
public final class DungeonCheckpointBeacon {
    private DungeonCheckpointBeacon() {}

    private static final int BEAM_HEIGHT = 48;
    private static final double MAX_DIST_SQ = 160.0 * 160.0;

    public static void tick(MinecraftServer server, ServerLevel dungeonLevel) {
        DungeonInstanceManager mgr = DungeonInstanceManager.get(dungeonLevel);
        for (DungeonInstance instance : mgr.getAllInstances()) {
            if (instance.getState() != DungeonInstance.State.ACTIVE) continue;
            StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
            if (stage == null || stage.checkpoints().isEmpty()) continue;
            for (ServerPlayer player : mgr.getPresentPlayers(instance, server)) {
                if (player.level() != dungeonLevel) continue;
                String latest = instance.getLatestCheckpointFor(player.getUUID());
                for (CheckpointData cp : stage.checkpoints()) {
                    if (!cp.isUnlocked(instance.getClearedNodes().contains(cp.nodeId()))) continue;
                    BlockPos pos = DungeonEntryRules.checkpointPosition(instance, cp);
                    double x = pos.getX() + 0.5, z = pos.getZ() + 0.5;
                    if (player.distanceToSqr(x, player.getY(), z) > MAX_DIST_SQ) continue;
                    for (int dy = 1; dy <= BEAM_HEIGHT; dy += 2) {
                        dungeonLevel.sendParticles(player, ParticleTypes.END_ROD, true,
                                x, pos.getY() + dy, z, 1, 0.05, 0.3, 0.05, 0.0);
                    }
                    if (cp.id().equals(latest)) {
                        dungeonLevel.sendParticles(player, ParticleTypes.HAPPY_VILLAGER, true,
                                x, pos.getY() + 1.0, z, 6, 0.6, 0.4, 0.6, 0.0);
                    }
                }
            }
        }
    }
}
