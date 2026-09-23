package com.piranport.debug;

import com.piranport.PiranPort;
import com.piranport.aviation.AircraftIndex;
import com.piranport.aviation.FireControlManager;
import com.piranport.combat.TransformationManager;
import com.piranport.entity.AircraftEntity;
import com.piranport.item.ShipCoreItem;
import com.piranport.network.DebugToggleAckPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PiranPort 调试系统。
 *
 * <p>采用 Per-player 会话模型：每个 OP 玩家独立开启一次调试会话，得到递增的
 * {@code sessionId}（如 {@code #1234}），所有日志和客户端反馈都带此编号。
 *
 * <p>每个会话写入独立文件 {@code logs/piranport-debug-<sessionId>.log}，
 * 通过 log4j2 {@link RollingFileAppender} + {@link AsyncAppender} 异步写入；
 * 关闭会话后文件重命名为 {@code .completed}，便于归档与上报。
 *
 * <p>公共埋点 API（向后兼容旧调用方）：
 * <pre>
 *   // 性能埋点（门控：仅在会话激活时记录）
 *   long t = System.nanoTime();
 *   // ... do work ...
 *   PiranPortDebug.perf("WeightScan", System.nanoTime() - t, "player=Steve weight=46/72");
 *
 *   // 事件埋点（自动注入 sessionId）
 *   PiranPortDebug.event("Transform ON | player={} core={} weight={}/{}", name, core, load, max);
 *
 *   // 错误埋点（永远记录，且包含 sessionId 若该玩家有活跃会话）
 *   PiranPortDebug.error("Aircraft ORPHAN | entityId={} reason={}", id, reason);
 *
 *   // 飞机生命周期埋点（由 AircraftEntity / ShipCoreCombat 直接调用）
 *   PiranPortDebug.aircraftLaunched(player, slot, stack, payloadType, attackMode, entityId);
 *   PiranPortDebug.aircraftReturnTriggered(entityId, reason, state, pos);
 *   PiranPortDebug.aircraftReturnItem(entityId, playerOnline, coreSlot, result);
 *   PiranPortDebug.aircraftRemoved(entityId, returned, lifetimeSeconds);
 * </pre>
 */
public final class PiranPortDebug {

    // Logger name matches the Logger name in log4j2.xml
    private static final Logger LOG = LogManager.getLogger("PiranPortDebug");

    private static final DateTimeFormatter ABS_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    // -------------------------------------------------------------------------
    // Configuration constants
    // -------------------------------------------------------------------------

    /** 单个分卷日志的滚动触发阈值：当前文件达到此大小即 rollover 到下一分卷 */
    public static final long SESSION_ROLL_BYTES = 50L * 1024L * 1024L; // 50 MB

    /** 单会话保留的分卷数（含当前文件）：总量 = SESSION_ROLL_BYTES × SESSION_ROLL_KEEP */
    public static final int SESSION_ROLL_KEEP = 8;

    /**
     * 单会话日志总量硬上限（含全部分卷）。
     * 由 {@link #SESSION_ROLL_BYTES} × {@link #SESSION_ROLL_KEEP} 决定，
     * 会话写入超过此量后旧分卷会被滚动覆盖，请以此为准判断磁盘占用上界。
     */
    public static final long MAX_SESSION_BYTES = SESSION_ROLL_BYTES * SESSION_ROLL_KEEP;

    /** 异步队列容量；满时丢弃（不阻塞业务线程） */
    public static final int ASYNC_QUEUE_CAPACITY = 1024;

    /** 归档保留：完整会话文件最多保留多少个（不含当前） */
    public static final int ARCHIVE_KEEP = 5;

    /** 会话超时：开启后超过此时间自动关闭 */
    public static final long SESSION_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30);

    /** 快照限频：同玩家两次快照最少间隔 */
    public static final long SNAPSHOT_COOLDOWN_MS = 5_000L;

    /** 快照默认扫描半径（格） */
    public static final int SNAPSHOT_RADIUS = 300;

    /** 快照限频：所有玩家共用的最小全局间隔，防止多 OP 并发叠加成主线程尖峰 */
    public static final long SNAPSHOT_GLOBAL_INTERVAL_MS = 1_000L;

    /** 生命周期日志专线：仅在存在活跃会话时记录，避免无会话时仍在主线程做 String.format */
    private static final java.util.concurrent.ExecutorService CLOSE_EXECUTOR =
            java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "PiranPortDebug-IO");
                t.setDaemon(true);
                return t;
            });

    // -------------------------------------------------------------------------
    // Per-player session state
    // -------------------------------------------------------------------------

    /** 递增的会话 ID 分配器 */
    private static final AtomicLong SESSION_COUNTER = new AtomicLong(1);

    /** 递增的操作 ID 分配器（开火/起降等可关联事件） */
    private static final AtomicLong OPERATION_COUNTER = new AtomicLong(1);

    /** 所有活跃会话：UUID → DebugSession */
    private static final Map<UUID, DebugSession> SESSIONS = new ConcurrentHashMap<>();

    /** 玩家最后一次快照时间戳（毫秒），用于限频 */
    private static final Map<UUID, Long> LAST_SNAPSHOT_MS = new ConcurrentHashMap<>();

    /** 全局快照限频时间戳：跨玩家共享，避免多人同时刷快照叠加成主线程尖峰 */
    private static final AtomicLong LAST_GLOBAL_SNAPSHOT_MS = new AtomicLong(0L);

    /** 冷却覆盖时长（5 秒 = 100 tick）— 由 {@link com.piranport.testtools.PiranPortTestTools} 实际使用 */
    public static final int COOLDOWN_OVERRIDE_TICKS = 100; // 5 seconds

    // Client-side toggle (set from ClientTickHandler F8 handler)
    private static volatile boolean clientEnabled = false;

    /** Called client-side by F8. Returns new client state. */
    public static boolean toggleClient() {
        clientEnabled = !clientEnabled;
        return clientEnabled;
    }

    public static boolean isClientEnabled() { return clientEnabled; }

    public static void setClientEnabled(boolean v) { clientEnabled = v; }

    /**
     * 向后兼容：返回是否至少有一个玩家开启了调试会话。
     * 旧代码用此判断"测试模式是否激活"，新代码应改用 {@link #isSessionActive(UUID)}。
     */
    public static boolean isServerEnabled() {
        return !SESSIONS.isEmpty();
    }

    // -------------------------------------------------------------------------
    // Toggle API (called by DebugTogglePayload handler)
    // -------------------------------------------------------------------------

    /**
     * 开启或关闭某玩家的调试会话。
     *
     * @return 会话结果：{@code OPENED} / {@code CLOSED} / {@code ALREADY_OPEN} / {@code ALREADY_CLOSED}
     */
    public static ToggleResult togglePlayer(UUID playerUuid, String playerName, boolean wantEnabled) {
        DebugSession existing = SESSIONS.get(playerUuid);
        if (wantEnabled) {
            if (existing != null) {
                return ToggleResult.ALREADY_OPEN.apply(existing.sessionId);
            }
            // 反向互斥：测试模式开启时拒绝开启调试会话，避免调试日志被冷却覆盖/免耗弹药污染。
            // 测试模式侧（PiranPortTestTools.toggleFor）已做正向互斥，此处补齐另一个方向。
            if (com.piranport.testtools.PiranPortTestTools.isTestModeActive()) {
                return ToggleResult.TEST_ACTIVE;
            }
            DebugSession session = openSession(playerUuid, playerName);
            return ToggleResult.OPENED.apply(session.sessionId);
        } else {
            if (existing == null) {
                return ToggleResult.ALREADY_CLOSED;
            }
            long sid = existing.sessionId;
            closeSession(playerUuid, CloseReason.USER);
            return ToggleResult.CLOSED.apply(sid);
        }
    }

    /** 关闭某玩家的会话（无会话则 no-op）。用于登出 / 服务端停止。 */
    public static void closePlayer(UUID playerUuid, CloseReason reason) {
        closeSession(playerUuid, reason);
    }

    /** 关闭所有会话。用于服务端停止。 */
    public static void closeAll(CloseReason reason) {
        for (UUID uuid : List.copyOf(SESSIONS.keySet())) {
            closeSession(uuid, reason);
        }
        // 服务端停止时清空限频残留，避免集成服务器反复开新世界时跨世界累积
        LAST_SNAPSHOT_MS.clear();
        LAST_GLOBAL_SNAPSHOT_MS.set(0L);
    }

    /** 强制清理过期的会话（用于周期任务）。 */
    public static void evictExpired() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, DebugSession> e : SESSIONS.entrySet()) {
            if (now - e.getValue().startTimeMs > SESSION_TIMEOUT_MS) {
                closeSession(e.getKey(), CloseReason.TIMEOUT);
            }
        }
    }

    public static boolean isSessionActive(UUID playerUuid) {
        return SESSIONS.containsKey(playerUuid);
    }

    public static long currentSessionId(UUID playerUuid) {
        DebugSession s = SESSIONS.get(playerUuid);
        return s == null ? -1L : s.sessionId;
    }

    public enum CloseReason { USER, LOGOUT, TIMEOUT, SERVER_STOP, REPLACED }

    // -------------------------------------------------------------------------
    // Test tool isolation (P2-9) — 冷却覆盖已迁移到 PiranPortTestTools
    // 此处保留向后兼容的 setCooldownOverride，内部委托给 TestTools。
    // -------------------------------------------------------------------------

    /**
     * @deprecated 冷却覆盖已迁移到 {@link com.piranport.testtools.PiranPortTestTools#toggleFor}，
     * 保留此方法仅为向后兼容。
     */
    @Deprecated
    public static void setCooldownOverride(boolean enabled) {
        // 注意：此方法无法获取玩家 UUID，因此仅做日志记录，不切换实际状态。
        // 新代码应通过 DebugCooldownOverridePayload 调用 PiranPortTestTools.toggleFor(uuid, enabled)。
        LOG.warn("[PiranPortDebug] setCooldownOverride({}) is deprecated; "
                + "use DebugCooldownOverridePayload with player UUID", enabled);
    }

    /**
     * @deprecated 冷却覆盖已迁移到 {@link com.piranport.testtools.PiranPortTestTools}，
     * 调用方应改用 {@code PiranPortTestTools.applyCooldownOverride}。
     * 保留此方法仅为向后兼容，内部委托到 TestTools。
     */
    @Deprecated
    public static int applyCooldownOverride(int ticks) {
        return com.piranport.testtools.PiranPortTestTools.applyCooldownOverride(ticks);
    }

    /**
     * @deprecated 改用 {@code PiranPortTestTools.applyCooldownOverride(UUID, int)}：
     * 只有测试模式属主才应享受覆盖，无 owner 版本会误伤全服玩家。
     */
    @Deprecated
    public static int applyCooldownOverride(UUID owner, int ticks) {
        return com.piranport.testtools.PiranPortTestTools.applyCooldownOverride(owner, ticks);
    }

    /**
     * @deprecated 同上，已迁移到 {@link com.piranport.testtools.PiranPortTestTools#isCooldownOverrideEnabled()}
     */
    @Deprecated
    public static boolean isCooldownOverrideEnabled() {
        return com.piranport.testtools.PiranPortTestTools.isCooldownOverrideEnabled();
    }

    /**
     * @deprecated 已迁移到 {@link com.piranport.testtools.PiranPortTestTools#isTestModeActive()}
     */
    @Deprecated
    public static boolean isTestModeActive() {
        return com.piranport.testtools.PiranPortTestTools.isTestModeActive();
    }

    /**
     * 测试模式（F8 debug ON）创造背包装填：调用方原本要 {@code stack.shrink(count)}，
     * 此处在测试模式下跳过消耗，等价于弹药/载荷无限。
     * 非测试模式时等价于 {@code stack.shrink(count)}。
     */
    /**
     * @deprecated consumeAmmo 已迁移到 {@link com.piranport.testtools.PiranPortTestTools#consumeAmmo}，
     * 调用方应改用 {@code PiranPortTestTools.consumeAmmo(stack, count)}。
     * 保留此方法仅为向后兼容。
     */
    @Deprecated
    public static void consumeAmmo(ItemStack stack, int count) {
        com.piranport.testtools.PiranPortTestTools.consumeAmmo(stack, count);
    }

    /**
     * @deprecated 改用 {@code PiranPortTestTools.consumeAmmo(UUID, stack, count)}：
     * 只有测试模式属主才应免耗弹药，无 owner 版本会让全服玩家一起无限弹药。
     */
    @Deprecated
    public static void consumeAmmo(UUID owner, ItemStack stack, int count) {
        com.piranport.testtools.PiranPortTestTools.consumeAmmo(owner, stack, count);
    }

    // -------------------------------------------------------------------------
    // Logging API
    // -------------------------------------------------------------------------

    /**
     * 性能埋点。仅在任一会话激活时记录（开销门控）。
     */
    public static void perf(String tag, long nanos, String context) {
        if (SESSIONS.isEmpty()) return;
        String msg = String.format(Locale.ROOT, "[PERF] %s | %dns | %s", tag, nanos, context);
        LOG.info(msg);
        for (DebugSession s : SESSIONS.values()) {
            s.stats.recordPerf(tag, nanos);
        }
    }

    /**
     * 事件埋点。仅在任一会话激活时记录。
     * 自动在前面拼接 {@code session=#N} 字段（取调用方玩家 UUID 的会话，否则留空）。
     */
    public static void event(String format, Object... args) {
        if (SESSIONS.isEmpty()) return;
        String body = format(format, args);
        String tag = body.split("[ |]")[0];
        for (DebugSession s : SESSIONS.values()) {
            s.stats.recordEvent(tag);
        }
        LOG.info("[EVENT] {}", body);
    }

    /**
     * 事件埋点的惰性变体：仅在有会话时才求值 {@code detail}。
     *
     * <p>用于高频路径（每 tick / 每循环槽位）的调用点——Java 是 eager evaluation，
     * 直接写 {@code event("...", expensive())} 会让实参在门控之外先行求值，
     * 即使用 {@code event()} 内部的 {@code SESSIONS.isEmpty()} 短路也挡不住。
     */
    public static void eventLazy(Object detail) {
        if (SESSIONS.isEmpty()) return;
        event(String.valueOf(detail));
    }

    /**
     * 是否应执行昂贵的埋点实参求值。调用方用法：
     * <pre>
     *   if (PiranPortDebug.shouldEmit()) {
     *       PiranPortDebug.event("... | weapon={}", expensiveLookup(), count);
     *   }
     * </pre>
     */
    public static boolean shouldEmit() {
        return !SESSIONS.isEmpty();
    }

    /**
     * 关联到具体玩家的 event 记录。自动注入该玩家的 sessionId，便于定位。
     */
    public static void eventFor(UUID playerUuid, String format, Object... args) {
        DebugSession s = SESSIONS.get(playerUuid);
        if (s == null) {
            // 无会话时直接短路，与 event() 行为对齐。
            // 原实现会 format 后 LOG.info 主日志，使得该入口在无会话时仍付出完整
            // String.format 代价，且 aircraft* 系列埋点全部经过这里。
            return;
        }
        String body = format(format, args);
        String tag = body.split("[ |]")[0];
        s.stats.recordEvent(tag);
        LOG.info("[EVENT] session=#{} | {}", s.sessionId, body);
    }

    /**
     * 错误埋点。永远记录（不受 enabled 门控），便于捕获严重异常。
     */
    public static void error(String format, Object... args) {
        String body = format(format, args);
        for (DebugSession s : SESSIONS.values()) {
            s.stats.recordError();
        }
        LOG.error("[ERROR] {}", body);
    }

    // -------------------------------------------------------------------------
    // Aircraft lifecycle embeds (P0-3)
    // -------------------------------------------------------------------------

    public static void aircraftLaunched(net.minecraft.world.entity.player.Player player, int weaponSlot, ItemStack stack,
                                        String payloadType, String attackMode, int entityId) {
        String stackHash = shortHash(stack);
        UUID uuid = player.getUUID();
        eventFor(uuid,
                "Aircraft LAUNCH | player={} slot={} stack={} payload={} mode={} entityId={} result={}",
                shortUuid(uuid), weaponSlot, stackHash, payloadType, attackMode, entityId, "SUCCESS");
    }

    public static void aircraftLaunchFailed(net.minecraft.world.entity.player.Player player, int weaponSlot, ItemStack stack,
                                            String reason) {
        eventFor(player.getUUID(),
                "Aircraft LAUNCH | player={} slot={} stack={} result=FAIL reason={}",
                shortUuid(player.getUUID()), weaponSlot, shortHash(stack), reason);
    }

    public static void aircraftReturnTriggered(int entityId, String reason,
                                               AircraftEntity.FlightState state, String pos) {
        // 不带玩家归属的全局事件，取第一个会话作为关联即可
        event("Aircraft RETURN_TRIGGER | entityId={} reason={} state={} pos={}",
                entityId, reason, state == null ? "?" : state.name(), pos == null ? "?" : pos);
    }

    public static void aircraftReturnItem(int entityId, UUID ownerUuid, boolean playerOnline,
                                          int coreSlot, String result) {
        eventFor(ownerUuid,
                "Aircraft RETURN_ITEM | entityId={} playerOnline={} coreSlot={} result={}",
                entityId, playerOnline, coreSlot, result);
    }

    public static void aircraftRemoved(int entityId, UUID ownerUuid, boolean returned, long lifetimeSec) {
        eventFor(ownerUuid,
                "Aircraft REMOVED | entityId={} returned={} lifetime={}s",
                entityId, returned, lifetimeSec);
    }

    // -------------------------------------------------------------------------
    // Failure embeds (P1-7)
    // -------------------------------------------------------------------------

    /**
     * 重量超载记录（{@code error()} 等级，不受 enabled 门控）。
     * 包含玩家短UUID、当前 / 最大重量、超载量、触发操作（可选）。
     */
    public static void weightOverload(net.minecraft.world.entity.player.Player player,
                                      int currentLoad, int maxLoad) {
        UUID uuid = player == null ? null : player.getUUID();
        int over = currentLoad - maxLoad;
        error("WeightScan OVERLOAD | player={} load={}/{} over={} dim={}",
                shortUuid(uuid), currentLoad, maxLoad, over,
                player == null ? "?" : player.level().dimension().location());
    }

    /**
     * 数据组件读取失败记录。包含物品 ID、组件类型、异常消息。
     */
    public static void componentReadFailed(String componentType, ItemStack stack, Throwable t) {
        String itemId = "empty";
        if (stack != null && !stack.isEmpty()) {
            itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        }
        String exMsg = t == null ? "?" : (t.getClass().getSimpleName() + ": " + t.getMessage());
        error("Component READ FAIL | component={} item={} ex={}",
                componentType, itemId, exMsg);
    }

    /**
     * 数据组件读取失败的便捷重载（不传异常）。
     */
    public static void componentReadFailed(String componentType, ItemStack stack, String reason) {
        String itemId = "empty";
        if (stack != null && !stack.isEmpty()) {
            itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        }
        error("Component READ FAIL | component={} item={} reason={}",
                componentType, itemId, reason);
    }

    /**
     * 网络包处理失败记录。包含包类型、玩家、异常类与消息。
     */
    public static void payloadFailed(String packetType,
                                     net.minecraft.world.entity.player.Player player, Throwable t) {
        UUID uuid = player == null ? null : player.getUUID();
        String exMsg = t == null ? "?" : (t.getClass().getSimpleName() + ": " + t.getMessage());
        error("Payload handler FAILED | type={} player={} ex={}",
                packetType, shortUuid(uuid), exMsg);
    }

    /**
     * 包裹一个 payload handler 主体，自动捕获并记录异常。
     * 用法：{@code ctx.enqueueWork(() -> PiranPortDebug.runPayload("DebugToggle",
     *         () -> PiranPortDebug.togglePlayer(...)));}
     */
    public static void runPayload(String packetType,
                                  net.minecraft.world.entity.player.Player player,
                                  Runnable body) {
        try {
            body.run();
        } catch (Throwable t) {
            payloadFailed(packetType, player, t);
            if (t instanceof RuntimeException re) throw re;
            throw new RuntimeException(t);
        }
    }

    // -------------------------------------------------------------------------
    // Operation ID helper (P1-6)
    // -------------------------------------------------------------------------

    /** 分配一个递增操作 ID，调用方将其嵌入日志便于跨日志关联 */
    public static long nextOperationId() {
        return OPERATION_COUNTER.getAndIncrement();
    }

    // -------------------------------------------------------------------------
    // Snapshot API
    // -------------------------------------------------------------------------

    /**
     * 写入一次完整快照。带限频：同一玩家 5 秒内只能触发一次。
     *
     * @return 限频剩余毫秒数（0 表示成功执行）
     */
    public static long snapshot(ServerPlayer player) {
        UUID uuid = player.getUUID();
        long now = System.currentTimeMillis();
        Long last = LAST_SNAPSHOT_MS.get(uuid);
        if (last != null && now - last < SNAPSHOT_COOLDOWN_MS) {
            return SNAPSHOT_COOLDOWN_MS - (now - last);
        }
        // 全局限频：per-UUID 冷却不防并发，N 名 OP 同时刷会在主线程叠加成周期性尖峰
        long globalLast = LAST_GLOBAL_SNAPSHOT_MS.get();
        if (now - globalLast < SNAPSHOT_GLOBAL_INTERVAL_MS) {
            return SNAPSHOT_GLOBAL_INTERVAL_MS - (now - globalLast);
        }
        LAST_SNAPSHOT_MS.put(uuid, now);
        LAST_GLOBAL_SNAPSHOT_MS.set(now);
        LOG.info(buildSnapshot(player));
        return 0;
    }

    // -------------------------------------------------------------------------
    // Session open / close
    // -------------------------------------------------------------------------

    private static DebugSession openSession(UUID playerUuid, String playerName) {
        long sid = SESSION_COUNTER.getAndIncrement();
        DebugSession s = new DebugSession(sid, playerUuid, playerName);
        // 1) 创建 per-session 异步 appender（内含 RollingFileAppender，必须先注册到 config）
        s.appender = createSessionAppender(sid);
        if (s.appender != null) {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();
            // AsyncAppender 内部引用的 RollingFileAppender 也注册到 config
            config.addAppender(findRolling(s.appender));
            config.addAppender(s.appender);
            LoggerConfig lc = config.getLoggerConfig("PiranPortDebug");
            lc.addAppender(s.appender, Level.INFO, null);
            ctx.updateLoggers();
        }
        SESSIONS.put(playerUuid, s);

        // 2) 输出开启横幅
        LOG.info("[SESSION] session=#{} player={} ({}) status=OPENED absTime={}",
                sid, playerName, shortUuid(playerUuid),
                ABS_TIME_FMT.format(Instant.now()));
        // 3) 客户端确认包由网络层（DebugTogglePayload）统一发送：
        //    这里再发一次会让每次开关都收到两条重复提示，且两份 sessionId 语义不一致。
        return s;
    }

    private static Appender findRolling(Appender async) {
        // AsyncAppender.getAppenders() 返回下游 appender 列表
        if (async instanceof AsyncAppender aa) {
            for (Appender a : aa.getAppenders()) {
                if (a instanceof RollingFileAppender) return a;
            }
        }
        return null;
    }

    private static void closeSession(UUID playerUuid, CloseReason reason) {
        DebugSession s = SESSIONS.remove(playerUuid);
        if (s == null) return;
        s.stats.flushSummary(s.sessionId);
        LOG.info("[SESSION] session=#{} reason={} status=CLOSED",
                s.sessionId, reason.name());

        // 1) 停止并解绑 appender（AsyncAppender 与其下游 RollingFileAppender 都要处理）
        if (s.appender != null) {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();
            LoggerConfig lc = config.getLoggerConfig("PiranPortDebug");
            // 先解绑 logger 上的 async，再停 async（停止投递），最后停 inner rolling
            lc.removeAppender(s.appender.getName());
            config.getAppenders().remove(s.appender.getName());
            Appender rolling = findRolling(s.appender);
            s.appender.stop();
            if (rolling != null) {
                // AsyncAppender.stop() 只停自身 dispatcher，不会停下游 appender。
                // 不显式 stop/remove 会永久残留 RollingFileAppender 与文件句柄。
                lc.removeAppender(rolling.getName());
                config.getAppenders().remove(rolling.getName());
                rolling.stop();
            }
            ctx.updateLoggers();
        }
        // 2) 归档本会话文件（重命名 + 清理全部分卷）改为后台执行，避免主 tick 线程做磁盘 I/O
        final long sid = s.sessionId;
        final Path baseDir = logsDir();
        CLOSE_EXECUTOR.execute(() -> {
            try {
                Path src = baseDir.resolve(sessionLogBase(sid) + ".log");
                Path dst = baseDir.resolve(sessionLogBase(sid) + ".log.completed");
                if (Files.exists(src)) {
                    Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
                }
                pruneArchives();
            } catch (Exception e) {
                PiranPort.LOGGER.warn("[PiranPortDebug] Failed to archive session log #{}: {}", sid, e.getMessage());
            }
        });
        // 3) 关闭确认包同样由网络层统一发送（防止重复提示 + 双份 sessionId 语义）
        // 4) 清理该玩家的快照限频记录，避免 UUID 条目无界累积与重连误限频
        LAST_SNAPSHOT_MS.remove(playerUuid);
    }

    // -------------------------------------------------------------------------
    // Per-session appender factory
    // -------------------------------------------------------------------------

    private static Appender createSessionAppender(long sessionId) {
        RollingFileAppender rolling = null;
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();

            PatternLayout layout = PatternLayout.newBuilder()
                    .withConfiguration(config)
                    .withPattern("[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%t] %msg%n")
                    .build();

            String fileName = "logs/piranport-debug-" + sessionId + ".log";
            String pattern = "logs/piranport-debug-" + sessionId + ".log.%i";
            String rollingName = "PPDebugRolling-" + sessionId;
            String asyncName = "PPDebugAsync-" + sessionId;

            // 显式滚动策略：固定窗口，最多 SESSION_ROLL_KEEP 个分卷（含当前文件），
            // 使单会话磁盘占用有明确上界 SESSION_ROLL_BYTES × SESSION_ROLL_KEEP。
            org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy rollover =
                    org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy.newBuilder()
                            .withConfig(config)
                            .withMax(String.valueOf(SESSION_ROLL_KEEP))
                            .withMin(String.valueOf(1))
                            .withFileIndex("min")
                            .build();
            SizeBasedTriggeringPolicy policy =
                    SizeBasedTriggeringPolicy.createPolicy(Long.toString(SESSION_ROLL_BYTES));

            rolling = RollingFileAppender.newBuilder()
                    .withConfiguration(config)
                    .withName(rollingName)
                    .withFileName(fileName)
                    .withFilePattern(pattern)
                    .withPolicy(policy)
                    .withStrategy(rollover)
                    .withLayout(layout)
                    .withIgnoreExceptions(true)
                    .build();
            rolling.start();

            // 关键顺序：AsyncAppender.start() 会通过 config.getAppenders() 解析下游引用，
            // 引用必须先在 Configuration 里注册，否则抛 ConfigurationException 并被下面的
            // catch 吞掉，导致整个 per-session 文件日志静默失效。
            config.addAppender(rolling);

            // AsyncAppender 通过 AppenderRef 引用已注册的 RollingFileAppender
            AsyncAppender async = AsyncAppender.newBuilder()
                    .setName(asyncName)
                    .setConfiguration(config)
                    .setBufferSize(ASYNC_QUEUE_CAPACITY)
                    .setBlocking(false)
                    .setIgnoreExceptions(true)
                    .setAppenderRefs(new AppenderRef[] {
                            AppenderRef.createAppenderRef(rollingName, Level.INFO, null) })
                    .build();
            async.start();
            return async;
        } catch (Throwable t) {
            PiranPort.LOGGER.warn("[PiranPortDebug] Failed to create session appender: {}", t.getMessage());
            // 失败回滚：rolling 可能已经 start() 并打开了文件句柄，必须显式释放
            if (rolling != null) {
                try {
                    LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
                    Configuration config = ctx.getConfiguration();
                    config.getAppenders().remove(rolling.getName());
                    rolling.stop();
                } catch (Throwable ignored) {}
            }
            return null;
        }
    }

    /**
     * 删除指定会话的所有日志文件：主文件、已完成归档、以及滚动产生的 .log.<i> 分卷。
     * 用于 pruneArchives 统一按「同一会话」为整体清理。
     */
    private static boolean deleteSessionLogs(File dir, String baseName) {
        boolean all = true;
        for (File f : java.util.Objects.requireNonNull(dir.listFiles())) {
            String n = f.getName();
            if (n.equals(baseName + ".log") || n.equals(baseName + ".log.completed")
                    || n.matches(java.util.regex.Pattern.quote(baseName) + "\\.log\\.\\d+")) {
                if (!f.delete()) all = false;
            }
        }
        return all;
    }

    /** 会话日志的基准文件名（不含扩展名） */
    private static String sessionLogBase(long sessionId) {
        return "piranport-debug-" + sessionId;
    }

    private static void pruneArchives() throws java.io.IOException {
        File dir = logsDir().toFile();
        // 归档单位是「会话」而不是「文件」：一个会话可能留下 .log / .log.completed / .log.<i> 多个文件，
        // 必须按会话整体保留/整体删除，否则滚动分卷会成为永不清理的孤儿。
        File[] all = dir.listFiles((d, n) -> n.startsWith("piranport-debug-"));
        if (all == null) return;

        Map<String, Long> latestBySession = new java.util.HashMap<>();
        for (File f : all) {
            String session = sessionBaseOf(f.getName());
            if (session == null) continue;
            latestBySession.merge(session, f.lastModified(), Math::max);
        }
        if (latestBySession.size() <= ARCHIVE_KEEP) return;

        List<String> sessions = new java.util.ArrayList<>(latestBySession.keySet());
        sessions.sort((a, b) -> Long.compare(latestBySession.get(b), latestBySession.get(a)));
        for (int i = ARCHIVE_KEEP; i < sessions.size(); i++) {
            String session = sessions.get(i);
            if (!deleteSessionLogs(dir, session)) {
                PiranPort.LOGGER.warn("[PiranPortDebug] Failed to delete old archive session {}", session);
            }
        }
    }

    /** 从日志文件名解析出会话基准名（{@code piranport-debug-<sid>}）；不匹配返回 null */
    private static String sessionBaseOf(String fileName) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^(piranport-debug-\\d+)\\.log(\\.[\\d]+|\\.completed)?$")
                .matcher(fileName);
        return m.matches() ? m.group(1) : null;
    }

    private static Path logsDir() {
        // 运行目录下的 logs/，与现有日志保持一致
        return Path.of("logs");
    }

    // -------------------------------------------------------------------------
    // Snapshot builder
    // -------------------------------------------------------------------------

    private static String buildSnapshot(ServerPlayer player) {
        StringBuilder sb = new StringBuilder();
        DebugSession s = SESSIONS.get(player.getUUID());
        long sid = s == null ? -1L : s.sessionId;

        sb.append("===== PiranPort Snapshot =====\n");
        sb.append("Session: #").append(sid).append('\n');
        sb.append("Timestamp: ").append(ABS_TIME_FMT.format(Instant.now())).append('\n');
        sb.append("Player: ").append(player.getName().getString())
          .append(" (").append(shortUuid(player.getUUID())).append(")\n");
        sb.append("Dimension: ").append(player.level().dimension().location()).append('\n');
        sb.append("Pos: ").append(String.format(Locale.ROOT, "[%.1f, %.1f, %.1f]",
                player.getX(), player.getY(), player.getZ())).append('\n');
        sb.append("Scan Radius: ").append(SNAPSHOT_RADIUS).append(" blocks\n");

        ItemStack coreStack = TransformationManager.findTransformedCore(player);
        boolean transformed = !coreStack.isEmpty();

        if (transformed) {
            ShipCoreItem sci = (ShipCoreItem) coreStack.getItem();
            String coreId = BuiltInRegistries.ITEM.getKey(sci).getPath();
            sb.append("Transform: ON (").append(coreId).append(")\n");
            int load = TransformationManager.getInventoryWeaponLoad(player.getInventory());
            int maxLoad = sci.getShipType().maxLoad;
            sb.append(String.format(Locale.ROOT, "Weight: %d/%d (%.1f%%)\n",
                    load, maxLoad, 100.0 * load / Math.max(1, maxLoad)));
            var speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null) {
                double speed = speedAttr.getValue() / 0.1;
                sb.append(String.format(Locale.ROOT, "MoveSpeed: %.3fx\n", speed));
            }
        } else {
            sb.append("Transform: OFF\n");
        }

        sb.append("Active Buffs:\n");
        var effects = player.getActiveEffects();
        if (effects.isEmpty()) {
            sb.append("  (none)\n");
        } else {
            for (var e : effects) {
                String name = e.getEffect().value().getDescriptionId();
                sb.append(String.format(Locale.ROOT, "  %s L%d (remaining: %ds)\n",
                        name, e.getAmplifier() + 1, e.getDuration() / 20));
            }
        }

        List<UUID> lockedTargets = FireControlManager.getTargets(player.getUUID());
        sb.append("Fire Control: ");
        if (lockedTargets.isEmpty()) {
            sb.append("(none)\n");
        } else {
            sb.append('\n');
            for (UUID uuid : lockedTargets) {
                Entity target = player.serverLevel().getEntity(uuid);
                if (target != null) {
                    sb.append(String.format(Locale.ROOT, "  %s #%d dist=%.1f\n",
                            target.getType().toShortString(), target.getId(),
                            player.distanceTo(target)));
                } else {
                    sb.append("  (gone) ").append(shortUuid(uuid)).append('\n');
                }
            }
        }

        // 当前维度附近：走已维护的 AircraftIndex 按属主查询，替代原先的 300 格全实体枚举。
        // 原因：getEntitiesOfClass 会对 600³ 方块体积做分区块实体枚举并逐个跑谓词，
        // 命中为 0 也要全扫，跑在服务端主线程上且被 5 秒冷却 × 多 OP 放大成 TPS 尖峰。
        sb.append("Active Aircraft (nearby, within ").append(SNAPSHOT_RADIUS).append("):\n");
        Set<AircraftEntity> owned = AircraftIndex.snapshot(player.getUUID());
        int globalCount = owned.size();
        int nearbyCount = 0;
        if (player.level() instanceof ServerLevel sl) {
            List<AircraftEntity> nearby = new java.util.ArrayList<>();
            for (AircraftEntity a : owned) {
                if (a.level() == sl
                        && a.blockPosition().distSqr(player.blockPosition())
                            <= (long) SNAPSHOT_RADIUS * SNAPSHOT_RADIUS) {
                    nearby.add(a);
                }
            }
            if (nearby.isEmpty()) {
                sb.append("  (none)\n");
            } else {
                int maxAircraft = 50; // P1-5: 大量飞机场景下截断并标注
                int shown = 0;
                for (AircraftEntity a : nearby) {
                    if (shown >= maxAircraft) {
                        sb.append("  ... (truncated, total=").append(nearby.size()).append(")\n");
                        break;
                    }
                    sb.append(String.format(Locale.ROOT, "  %s entityId=%d state=%s\n",
                            a.getAircraftType().name(), a.getId(), a.getFlightState().name()));
                    shown++;
                }
            }
            nearbyCount = nearby.size();
        }

        // P1-5: 全服飞机统计（任何维度）
        sb.append("All owned aircraft (any dimension): ").append(globalCount).append('\n');
        sb.append("Nearby total: ").append(nearbyCount).append('\n');

        sb.append("========================================");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    public static String shortUuid(UUID uuid) {
        if (uuid == null) return "?";
        String s = uuid.toString().replace("-", "");
        return s.length() > 8 ? s.substring(0, 8) : s;
    }

    private static String shortHash(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "empty";
        return String.format(Locale.ROOT, "%08x", stack.hashCode());
    }

    private static String format(String fmt, Object... args) {
        try {
            return String.format(Locale.ROOT, fmt, args);
        } catch (Throwable t) {
            // 兜底：format 失败时直接拼接，避免业务崩溃
            StringBuilder sb = new StringBuilder(fmt);
            sb.append(" | args=");
            for (Object o : args) sb.append(String.valueOf(o)).append(',');
            return sb.toString();
        }
    }

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    /** Toggle 结果枚举，含 sessionId 便于客户端展示 */
    public record ToggleResult(boolean enabled, long sessionId, String status) {
        public static final ToggleResult OPENED = new ToggleResult(true, -1L, "OPENED");
        public static final ToggleResult CLOSED = new ToggleResult(false, -1L, "CLOSED");
        public static final ToggleResult ALREADY_OPEN = new ToggleResult(true, -1L, "ALREADY_OPEN");
        public static final ToggleResult ALREADY_CLOSED = new ToggleResult(false, -1L, "ALREADY_CLOSED");
        /** 反向互斥：测试模式正在运行，拒绝开启调试会话 */
        public static final ToggleResult TEST_ACTIVE = new ToggleResult(false, -1L, "TEST_ACTIVE");

        public ToggleResult apply(long sid) {
            return new ToggleResult(enabled, sid, status);
        }
    }

    private static final class DebugSession {
        final long sessionId;
        final UUID playerUuid;
        final String playerName;
        final long startTimeMs;
        final DebugStats stats = new DebugStats();
        volatile Appender appender; // assigned at openSession

        DebugSession(long sessionId, UUID playerUuid, String playerName) {
            this.sessionId = sessionId;
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.startTimeMs = System.currentTimeMillis();
        }
    }

    private static final class DebugStats {
        private final Map<String, PerfStat> perfStats = new ConcurrentHashMap<>();
        private final Map<String, AtomicLong> eventCounts = new ConcurrentHashMap<>();
        private final AtomicLong errorCount = new AtomicLong(0);

        void recordPerf(String tag, long nanos) {
            perfStats.computeIfAbsent(tag, k -> new PerfStat()).record(nanos);
        }

        void recordEvent(String tag) {
            eventCounts.computeIfAbsent(tag, k -> new AtomicLong()).incrementAndGet();
        }

        void recordError() {
            errorCount.incrementAndGet();
        }

        void flushSummary(long sessionId) {
            long elapsed = (System.currentTimeMillis() - startTimeMs) / 1000;
            StringBuilder sb = new StringBuilder();
            sb.append("===== PiranPort Debug Summary (#").append(sessionId).append(") =====\n");
            sb.append("Session duration: ").append(elapsed).append("s\n");
            sb.append("--- Performance ---\n");
            if (perfStats.isEmpty()) {
                sb.append("  (no samples)\n");
            } else {
                perfStats.forEach((tag, stat) -> sb.append(
                        String.format(Locale.ROOT, "  %-20s avg=%dns  max=%dns  count=%d%n",
                                tag + ":", stat.avg(), stat.max, stat.count)));
            }
            sb.append("--- Events ---\n");
            if (eventCounts.isEmpty()) {
                sb.append("  (none)\n");
            } else {
                eventCounts.forEach((tag, count) -> sb.append(
                        String.format(Locale.ROOT, "  %-20s %d%n", tag + ":", count.get())));
            }
            sb.append(String.format(Locale.ROOT, "--- Errors ---\nTotal: %d%n", errorCount.get()));
            sb.append("=======================================");
            LOG.info(sb.toString());
        }

        private final long startTimeMs = System.currentTimeMillis();
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