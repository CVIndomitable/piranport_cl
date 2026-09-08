package com.piranport.dungeon.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 副本记录点隐形 marker（整合版 §3.2）。
 *
 * <p>玩家进入方块边界 → 触发服务端 {@link com.piranport.dungeon.event.DungeonEventHandler#onCheckpointReached}。</p>
 *
 * <p>这是占位实现：阶段 3 (P1-B) 完整流程要求 BE/region 触发；当前最小版本由本 Block 在 tick 中检测玩家。</p>
 */
public class DungeonCheckpointBlock extends Block {
    /** 半格尺寸的碰撞箱（玩家踩到即触发） */
    private static final AABB HITBOX = new AABB(0, 0, 0, 1, 1, 1);

    public DungeonCheckpointBlock(Properties props) {
        super(props);
    }

    /**
     * 玩家碰撞 → 触发服务端记录点逻辑。整合版 §3.2：踩到记录点后客户端展示光柱+标题+音效（已由 CheckpointReachedPayload S2C 处理）。
     */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide()) return;
        if (!(entity instanceof Player player)) return;

        com.piranport.dungeon.event.DungeonEventHandler.onCheckpointReached(
                (net.minecraft.server.level.ServerPlayer) player, pos);
    }

    /** 用于客户端 hit detection。 */
    public static AABB getHitbox() {
        return HITBOX;
    }
}