package com.piranport.dungeon.instance;

import com.piranport.PiranPort;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.TerrainType;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 副本建造的后台进度播报（服务端控制台）。
 *
 * <p>副本实例的地形由 {@link TerrainGenerationPipeline} 分帧推进：先铺共享的方块基底
 * （海水/海床）与整图屏障环，再按节点写地形特征、POI（出生平台与记录点）和节点屏障环；
 * 地形就绪后玩家才进本，进本时才刷怪。这些阶段原先在控制台完全静默，玩家只能靠书台纹路
 * 颜色猜建造进度。本类在阶段切换、每跨越 10% 进度时各打一行中文日志，并在完成时打一行总耗时，
 * 便于服务器后台确认「卡在哪一步」。</p>
 *
 * <p>日志一律走 {@link PiranPort#LOGGER}，按实例 + 节点键控做节流：同一键 20 tick 内最多一行，
 * 避免分帧推进的每 tick 都刷屏。</p>
 */
public final class TerrainGenerationProgress {
    /** 同一实例同一节点两次进度行之间的最小间隔（tick），20 tick = 1 秒。 */
    private static final int MIN_INTERVAL_TICKS = 20;
    /** 进度再跨过该比例即打一行（0.10 = 每 10%）。 */
    private static final double STEP = 0.10;
    /** 进度表容量上限：异常中断（永不就绪）的建造不会永久占用内存。 */
    private static final int MAX_TRACKS = 64;

    private static final Map<String, Track> TRACKS = new HashMap<>();

    private TerrainGenerationProgress() {}

    private static final class Track {
        final long startTick;
        final long total;
        double reported;      // 已播报到的进度（对齐到 STEP 的整数倍）
        String phase = "";    // 已播报到的阶段名
        long lastLogTick;
        long lastSeenTick;

        Track(long startTick, long total, long now) {
            this.startTick = startTick;
            this.total = total;
            this.lastSeenTick = now;
            // 允许首帧立即播报（否则世界刚启动 tick 计数 < MIN_INTERVAL_TICKS 时首行会被吞掉）。
            this.lastLogTick = now - MIN_INTERVAL_TICKS;
        }
    }

    /** 实例刚创建时的「阶段 0」播报：区域已经分配好，下面开始分帧铺地形。 */
    public static void onInstanceCreated(UUID instanceId, String stageId, int regionIndex) {
        PiranPort.LOGGER.info("[副本生成] 实例 {} 关卡 {}：维度 {} 区域已分配（索引 {}），开始分帧建造起点地形",
                shortId(instanceId), stageId,
                com.piranport.dungeon.event.DungeonEventHandler.DUNGEON_DIMENSION.location(),
                regionIndex);
    }

    /** 节点敌人刷出后的「生成生物」播报。 */
    public static void onEnemiesSpawned(DungeonInstance instance, NodeData node, int count, String enemySetId) {
        PiranPort.LOGGER.info("[副本生成] 实例 {} 关卡 {} 节点 {} | 阶段：生成生物（{}） | 共生成 {} 个敌人",
                shortId(instance.getInstanceId()), instance.getStageId(), node.nodeId(),
                enemySetId == null ? "无敌人配置" : enemySetId, count);
    }

    /**
     * 每 tick 由 {@link TerrainGenerationPipeline#tick} 调用。跨越阶段或整百分比且有节流余量时打一行，
     * 地形就绪时打一行总耗时并丢弃追踪记录。
     */
    public static void report(ServerLevel level, DungeonInstance instance, NodeData node) {
        if (instance == null || node == null) return;
        long now = level.getServer().getTickCount();
        String key = instance.getInstanceId() + "|" + node.nodeId();
        long remaining = TerrainGenerationPipeline.queuedBlocks(level, instance, node);
        boolean ready = TerrainGenerationPipeline.isReady(level, instance, node);

        Track track = TRACKS.get(key);
        if (track == null) {
            if (ready) return; // 无追踪记录且已就绪：说明是读档续跑，不补报
            track = new Track(now, Math.max(remaining, 1L), now);
            TRACKS.put(key, track);
            prune(now);
        }
        track.lastSeenTick = now;

        if (ready) {
            TRACKS.remove(key);
            double seconds = (now - track.startTick) / 20.0;
            PiranPort.LOGGER.info("[副本生成] 实例 {} 关卡 {} 节点 {} | 地形建造完成 | 共约 {} 方块 | 耗时 {}（{} tick）",
                    shortId(instance.getInstanceId()), instance.getStageId(), node.nodeId(),
                    track.total, formatSeconds(seconds), now - track.startTick);
            return;
        }

        double fraction = 1.0 - (double) remaining / track.total;
        if (fraction < 0) fraction = 0;
        if (fraction > 1) fraction = 1;
        String phase = phaseName(level, instance, node);
        boolean phaseChanged = !phase.equals(track.phase);
        boolean stepCrossed = fraction >= track.reported + STEP;
        if ((phaseChanged || stepCrossed) && now - track.lastLogTick >= MIN_INTERVAL_TICKS) {
            track.phase = phase;
            track.reported = Math.floor(fraction / STEP) * STEP;
            track.lastLogTick = now;
            PiranPort.LOGGER.info("[副本生成] 实例 {} 关卡 {} 节点 {} | 阶段：{} | 进度 {}% | 剩余约 {} 方块 | 已用 {}",
                    shortId(instance.getInstanceId()), instance.getStageId(), node.nodeId(),
                    phase, (int) Math.round(fraction * 100), remaining,
                    formatSeconds((now - track.startTick) / 20.0));
        }
    }

    /**
     * 当前实际在写哪个阶段：共享基底未就绪时一律先报基底状态（含它顺带走的整图边界），
     * 基底就绪后才轮到该节点的局部地形。
     */
    private static String phaseName(ServerLevel level, DungeonInstance instance, NodeData node) {
        TerrainGenerationState base = TerrainGenerationPipeline.state(level, instance, null);
        if (base.phase() != TerrainGenerationState.Phase.READY) {
            return switch (base.phase()) {
                case BASE -> "方块基底（共享海床）";
                case FEATURES -> "地形特征（共享）";
                case POI -> "POI（共享，出生平台/记录点）";
                case BOUNDARY -> "边界屏障环（整图）";
                case READY -> "就绪";
            };
        }
        TerrainGenerationState local = TerrainGenerationPipeline.state(level, instance, node);
        return switch (local.phase()) {
            case BASE -> "方块基底（节点）";
            case FEATURES -> "地形特征（节点：" + terrainName(node) + "）";
            case POI -> "POI（节点，出生平台/记录点）";
            case BOUNDARY -> "边界屏障环（节点）";
            case READY -> "就绪";
        };
    }

    private static String terrainName(NodeData node) {
        TerrainType terrain = node.terrainType();
        return terrain == null ? TerrainType.T1_OCEAN.displayName() : terrain.displayName();
    }

    /** 秒数保留一位小数。 */
    private static String formatSeconds(double seconds) {
        return String.format(java.util.Locale.ROOT, "%.1f 秒", seconds);
    }

    /** 追踪表超限时丢弃最久未更新的条目。 */
    private static void prune(long now) {
        if (TRACKS.size() <= MAX_TRACKS) return;
        long cutoff = now - 20L * 60; // 1 分钟未再推进的建造视为已中断
        Iterator<Map.Entry<String, Track>> it = TRACKS.entrySet().iterator();
        while (it.hasNext() && TRACKS.size() > MAX_TRACKS) {
            Map.Entry<String, Track> entry = it.next();
            if (entry.getValue().lastSeenTick < cutoff) it.remove();
        }
    }

    /** 实例 id 只取前 8 位，避免每次刷屏整条 UUID。 */
    public static String shortId(UUID instanceId) {
        String raw = instanceId.toString();
        return raw.substring(0, 8);
    }
}
