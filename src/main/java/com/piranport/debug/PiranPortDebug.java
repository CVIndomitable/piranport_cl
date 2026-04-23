package com.piranport.debug;

import com.piranport.aviation.FireControlManager;
import com.piranport.combat.TransformationManager;
import com.piranport.entity.AircraftEntity;
import com.piranport.item.ShipCoreItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PiranPort 调试系统。
 * <p>
 * 服务端: 通过 C2S 包 {@code DebugTogglePayload} 开启/关闭；
 * 日志写入独立文件 {@code logs/piranport-debug.log}（log4j2.xml 配置）。
 * <p>
 * 客户端: F8 切换水印; Shift+F8 请求快照。
 * <p>
 * 使用方法:
 * <pre>
 *   // 性能埋点
 *   long t = System.nanoTime();
 *   // ... do work ...
 *   PiranPortDebug.perf("WeightScan", System.nanoTime() - t, "player=" + name);
 *
 *   // 事件埋点
 *   PiranPortDebug.event("Transform ON | player={} core={} weight={}/{}", name, core, load, max);
 *
 *   // 错误埋点（不受 enabled 门控）
 *   PiranPortDebug.error("Aircraft ORPHAN | entityId={} reason={}", id, reason);
 * </pre>
 */
public final class PiranPortDebug {

    // Logger name must match the Logger name in log4j2.xml
    private static final Logger LOG = LogManager.getLogger("PiranPortDebug");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    // Server-side toggle (set from DebugTogglePayload handler)
    private static volatile boolean serverEnabled = false;

    // Client-side toggle (set from ClientTickHandler F8 handler)
    private static volatile boolean clientEnabled = false;

    // Debug-only: when true, all weapon/slot cooldowns are clamped to 5s (100 ticks).
    // Server-side state, toggled via DebugCooldownOverridePayload.
    private static volatile boolean cooldownOverrideEnabled = false;
    public static final int COOLDOWN_OVERRIDE_TICKS = 100; // 5 seconds

    private static final DebugStats stats = new DebugStats();

    // -------------------------------------------------------------------------
    // Toggle
    // -------------------------------------------------------------------------

    /** Called server-side when toggle packet arrives. */
    public static void setServerEnabled(boolean enabled) {
        if (!enabled && serverEnabled) {
            writeSummary();
        }
        serverEnabled = enabled;
        if (enabled) {
            stats.reset();
            LOG.info("===== PiranPort Debug ENABLED at {} =====", LocalTime.now().format(TIME_FMT));
        } else {
            LOG.info("===== PiranPort Debug DISABLED at {} =====", LocalTime.now().format(TIME_FMT));
        }
    }

    /** Called client-side by F8. Returns new client state. */
    public static boolean toggleClient() {
        clientEnabled = !clientEnabled;
        return clientEnabled;
    }

    public static boolean isServerEnabled() { return serverEnabled; }
    public static boolean isClientEnabled() { return clientEnabled; }

    /** Debug cooldown override — clamps every weapon cooldown to 5 seconds on the server. */
    public static void setCooldownOverride(boolean enabled) {
        cooldownOverrideEnabled = enabled;
        LOG.info("===== PiranPort Cooldown Override {} at {} =====",
                enabled ? "ENABLED" : "DISABLED", LocalTime.now().format(TIME_FMT));
    }

    public static boolean isCooldownOverrideEnabled() { return cooldownOverrideEnabled; }

    /**
     * 测试模式（F8 debug ON）创造背包装填：调用方原本要 stack.shrink(count)，
     * 此处在测试模式下跳过消耗，等价于弹药/载荷无限。
     * 非测试模式时等价于 stack.shrink(count)。
     */
    public static void consumeAmmo(ItemStack stack, int count) {
        if (!serverEnabled) stack.shrink(count);
    }

    /**
     * Applies the debug cooldown override. When enabled, clamps {@code ticks} to
     * {@link #COOLDOWN_OVERRIDE_TICKS} (5 seconds). Otherwise returns {@code ticks} unchanged.
     */
    public static int applyCooldownOverride(int ticks) {
        if (cooldownOverrideEnabled && ticks > COOLDOWN_OVERRIDE_TICKS) {
            return COOLDOWN_OVERRIDE_TICKS;
        }
        return ticks;
    }

    // -------------------------------------------------------------------------
    // Logging
    // -------------------------------------------------------------------------

    /**
     * Performance timing embed.
     *
     * @param tag     埋点标签，如 "WeightScan"
     * @param nanos   耗时（纳秒）
     * @param context 附加键值对字符串，如 "player=Steve weight=46/72"
     */
    public static void perf(String tag, long nanos, String context) {
        if (!serverEnabled) return;
        LOG.info("[PERF] {} | {}ns | {}", tag, nanos, context);
        stats.recordPerf(tag, nanos);
    }

    /**
     * Event embed. Format string is a log4j2-style pattern.
     *
     * @param format 如 "Transform ON | player={} core={} weight={}/{}"
     * @param args   参数列表
     */
    public static void event(String format, Object... args) {
        if (!serverEnabled) return;
        LOG.info("[EVENT] " + format, args);
        // Extract first word before space or '|' as the event tag for stats
        String tag = format.split("[ |]")[0];
        stats.recordEvent(tag);
    }

    /**
     * Error embed. Always logged regardless of enabled state.
     */
    public static void error(String format, Object... args) {
        LOG.error("[ERROR] " + format, args);
        stats.recordError();
    }

    // -------------------------------------------------------------------------
    // Snapshot (always runs, independent of enabled)
    // -------------------------------------------------------------------------

    /** Write a full state snapshot for the player. Called from server on Shift+F8 request. */
    public static void snapshot(ServerPlayer player) {
        LOG.info(buildSnapshot(player));
    }

    // -------------------------------------------------------------------------
    // Summary
    // -------------------------------------------------------------------------

    private static void writeSummary() {
        LOG.info(stats.buildSummary());
    }

    // -------------------------------------------------------------------------
    // Snapshot builder
    // -------------------------------------------------------------------------

    private static String buildSnapshot(ServerPlayer player) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== PiranPort Snapshot ").append(LocalTime.now().format(TIME_FMT)).append(" =====\n");
        sb.append("Player: ").append(player.getName().getString()).append("\n");

        ItemStack coreStack = TransformationManager.findTransformedCore(player);
        boolean transformed = !coreStack.isEmpty();

        if (transformed) {
            ShipCoreItem sci = (ShipCoreItem) coreStack.getItem();
            String coreId = BuiltInRegistries.ITEM.getKey(sci).getPath();
            sb.append("Transform: ON (").append(coreId).append(")\n");
            int load = TransformationManager.getInventoryWeaponLoad(player.getInventory());
            int maxLoad = sci.getShipType().maxLoad;
            sb.append(String.format("Weight: %d/%d (%.1f%%)\n", load, maxLoad, 100.0 * load / maxLoad));
            var speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null) {
                double speed = speedAttr.getValue() / 0.1;
                sb.append(String.format("MoveSpeed: %.3fx\n", speed));
            }
        } else {
            sb.append("Transform: OFF\n");
        }

        // Active effects
        sb.append("Active Buffs:\n");
        var effects = player.getActiveEffects();
        if (effects.isEmpty()) {
            sb.append("  (none)\n");
        } else {
            for (var e : effects) {
                String name = e.getEffect().value().getDescriptionId();
                sb.append(String.format("  %s L%d (remaining: %ds)\n",
                        name, e.getAmplifier() + 1, e.getDuration() / 20));
            }
        }

        // Fire control targets
        List<UUID> lockedTargets = FireControlManager.getTargets(player.getUUID());
        sb.append("Fire Control: ");
        if (lockedTargets.isEmpty()) {
            sb.append("(none)\n");
        } else {
            sb.append("\n");
            for (UUID uuid : lockedTargets) {
                Entity target = player.serverLevel().getEntity(uuid);
                if (target != null) {
                    sb.append(String.format("  %s #%d dist=%.1f\n",
                            target.getType().toShortString(), target.getId(),
                            player.distanceTo(target)));
                } else {
                    sb.append("  (gone) ").append(uuid).append("\n");
                }
            }
        }

        // Active aircraft
        sb.append("Active Aircraft:\n");
        if (player.level() instanceof ServerLevel sl) {
            List<AircraftEntity> aircraft = sl.getEntitiesOfClass(
                    AircraftEntity.class,
                    player.getBoundingBox().inflate(300),
                    a -> player.getUUID().equals(a.getOwnerUUID()));
            if (aircraft.isEmpty()) {
                sb.append("  (none)\n");
            } else {
                for (AircraftEntity a : aircraft) {
                    sb.append(String.format("  %s entityId=%d state=%s\n",
                            a.getAircraftType().name(), a.getId(), a.getFlightState().name()));
                }
            }
        }

        sb.append("========================================");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Stats accumulator
    // -------------------------------------------------------------------------

    private static final class DebugStats {
        private final Map<String, PerfStat> perfStats = new ConcurrentHashMap<>();
        private final Map<String, AtomicLong> eventCounts = new ConcurrentHashMap<>();
        private final AtomicLong errorCount = new AtomicLong(0);
        private volatile long startTimeMs = System.currentTimeMillis();

        void recordPerf(String tag, long nanos) {
            perfStats.computeIfAbsent(tag, k -> new PerfStat()).record(nanos);
        }

        void recordEvent(String tag) {
            eventCounts.computeIfAbsent(tag, k -> new AtomicLong()).incrementAndGet();
        }

        void recordError() {
            errorCount.incrementAndGet();
        }

        void reset() {
            perfStats.clear();
            eventCounts.clear();
            errorCount.set(0);
            startTimeMs = System.currentTimeMillis();
        }

        String buildSummary() {
            long elapsed = (System.currentTimeMillis() - startTimeMs) / 1000;
            StringBuilder sb = new StringBuilder();
            sb.append("===== PiranPort Debug Summary =====\n");
            sb.append("Session: ").append(elapsed).append("s\n");
            sb.append("--- Performance ---\n");
            if (perfStats.isEmpty()) {
                sb.append("  (no samples)\n");
            } else {
                perfStats.forEach((tag, stat) -> sb.append(
                        String.format("  %-20s avg=%dns  max=%dns  count=%d\n",
                                tag + ":", stat.avg(), stat.max, stat.count)));
            }
            sb.append("--- Events ---\n");
            if (eventCounts.isEmpty()) {
                sb.append("  (none)\n");
            } else {
                eventCounts.forEach((tag, count) -> sb.append(
                        String.format("  %-20s %d\n", tag + ":", count.get())));
            }
            sb.append(String.format("--- Errors ---\nTotal: %d\n", errorCount.get()));
            sb.append("=======================================");
            return sb.toString();
        }
    }

    private static final class PerfStat {
        long count, sum, max;

        synchronized void record(long nanos) {
            count++;
            sum += nanos;
            if (nanos > max) max = nanos;
        }

        synchronized long avg() { return count > 0 ? sum / count : 0; }
    }
}
