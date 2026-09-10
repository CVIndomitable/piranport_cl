package com.piranport.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

/**
 * Boss 战环境破坏器 — 策划决策/副本/07-Boss战环境防卡地形设计.md
 *
 * <p>实现方案 B：环境自带爆炸与变动，防止玩家用黑曜石/气泡柱/末影水晶等
 * "固定设备"困住 Boss。每隔 {@link #TICK_INTERVAL} tick 在 Boss 周围
 * 半径 {@link #RADIUS} 内执行：</p>
 * <ul>
 *   <li>爆破黑曜石/哭泣的黑曜石/气泡柱方块（还原为空气）</li>
 *   <li>触发小型原版爆炸（仅伤害玩家方块破坏范围，不破坏地形防止连锁）</li>
 *   <li>召唤 3-5 个小型环境冲击（火山喷发/海流冲击的占位效果）</li>
 * </ul>
 *
 * <p>仅在副本 Boss 房间关卡脚本调用 {@link #tick}；调用方需传入 Boss 实体引用。</p>
 */
public final class BossAntiStuckArea {

    /** 每 100 tick（5 秒）执行一次环境破坏。 */
    public static final int TICK_INTERVAL = 100;
    /** 影响半径（围绕 Boss 中心）。 */
    public static final int RADIUS = 24;
    /** 环境中爆破的方块列表。 */
    private static final Set<net.minecraft.world.level.block.Block> DISSOLVE_BLOCKS = new HashSet<>();

    static {
        DISSOLVE_BLOCKS.add(Blocks.OBSIDIAN);
        DISSOLVE_BLOCKS.add(Blocks.CRYING_OBSIDIAN);
        DISSOLVE_BLOCKS.add(Blocks.BUBBLE_COLUMN);
    }

    private BossAntiStuckArea() {}

    /**
     * 在 Boss 周围触发一次环境破坏 tick。
     * 副本 Boss 房间关卡脚本应在玩家进入房间后定时调用此方法。
     *
     * @param level 服务端世界
     * @param boss Boss 实体
     * @return 实际破坏的方块数
     */
    public static int tick(ServerLevel level, LivingEntity boss) {
        if (level == null || boss == null || !boss.isAlive()) return 0;
        AABB area = boss.getBoundingBox().inflate(RADIUS);
        BlockPos center = boss.blockPosition();

        // 1) 销毁黑曜石/气泡柱方块（末影水晶作为实体由玩家主动拆除）
        int destroyed = 0;
        for (BlockPos p : BlockPos.betweenClosed(
                (int) area.minX, (int) area.minY, (int) area.minZ,
                (int) area.maxX, (int) area.maxY, (int) area.maxZ)) {
            if (center.distSqr(p) > (double) RADIUS * RADIUS) continue;
            var state = level.getBlockState(p);
            if (DISSOLVE_BLOCKS.contains(state.getBlock())) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                destroyed++;
            }
        }

        // 2) 周期性小型环境爆炸（火山喷发/海流冲击的占位效果）
        // 仅在 Boss 附近随机 1-2 个点，爆炸不破坏方块（NONE 交互），仅作粒子和音效
        if (level.getGameTime() % (TICK_INTERVAL * 2L) == 0L) {
            for (int i = 0; i < 2; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                double r = 8 + level.random.nextDouble() * 12;
                BlockPos target = center.offset(
                        (int) (Math.cos(angle) * r),
                        level.random.nextInt(-2, 5),
                        (int) (Math.sin(angle) * r));
                level.explode(null, target.getX() + 0.5, target.getY() + 0.5,
                        target.getZ() + 0.5, 1.2f, Level.ExplosionInteraction.NONE);
            }
        }

        // 3) 海流冲击占位效果（粒子）— 周期在 Boss 周围洒水粒子
        if (level.getGameTime() % (TICK_INTERVAL / 2L) == 0L) {
            for (int i = 0; i < 8; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                double r = 6 + level.random.nextDouble() * 16;
                double yOffset = level.random.nextDouble() * 8;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,
                        center.getX() + Math.cos(angle) * r,
                        center.getY() + yOffset,
                        center.getZ() + Math.sin(angle) * r,
                        1, 0, 0.2, 0, 0.05);
            }
        }

        return destroyed;
    }

    /** 关卡侧 boss 房间 tick 钩子：每 TICK_INTERVAL tick 调用一次 tick()。 */
    public static void maybeTick(ServerLevel level, LivingEntity boss) {
        if (level == null || boss == null) return;
        if (level.getGameTime() % TICK_INTERVAL != 0L) return;
        tick(level, boss);
    }
}