package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.config.ConfigToolPermissions;
import com.piranport.item.TorpedoItem;
import com.piranport.terminal.TerminalOverridesSavedData;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 更新调试终端速度覆盖网络包（客户端 → 服务端）。
 *
 * <p>一个包同时承载两种域：鱼雷型号级偏移（float，单位 blocks/tick）
 * 与舰娘核心舰型级倍率偏移（double）。WHY 不拆成两个包：
 * 分类只是字符串多一个字节，拆开则要维护两套 handler、两套权限检查、两套错误提示，
 * 而两域的生命周期（单机调试、重置全部）完全一致。
 */
public record UpdateTerminalOverridePayload(
        String category,  // "torpedo" 或 "core"
        String key,       // 鱼雷注册 ID 或舰型名
        double delta      // 偏移量；两个域都走 double 以免再分出第三条编解码分支
) implements CustomPacketPayload {

    public static final String CATEGORY_TORPEDO = "torpedo";
    public static final String CATEGORY_CORE = "core";

    public static final Type<UpdateTerminalOverridePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "update_terminal_override"));

    /** 限长防止畸形包撑爆内存；注册 ID 与舰型名都远短于此。 */
    private static final int MAX_KEY_LENGTH = 128;

    public static final StreamCodec<ByteBuf, UpdateTerminalOverridePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_KEY_LENGTH), UpdateTerminalOverridePayload::category,
            ByteBufCodecs.stringUtf8(MAX_KEY_LENGTH), UpdateTerminalOverridePayload::key,
            ByteBufCodecs.DOUBLE, UpdateTerminalOverridePayload::delta,
            UpdateTerminalOverridePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateTerminalOverridePayload payload, IPayloadContext context) {
        // runPayload：与其余调试通道一致地兜住异常并附带玩家上下文，
        // 裸 enqueueWork 一旦抛异常会静默吞掉，客户端停在乐观更新后的假状态。
        com.piranport.debug.PiranPortDebug.runPayload("UpdateTerminalOverride", context.player(), () -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }

            // 只考虑单机，但权限门禁保留：局域网开放时非房主也连得上，
            // 不能因为「设计上单机」就默认每个连接都有管理员权限。
            if (!ConfigToolPermissions.canUse(serverPlayer)) {
                PiranPort.LOGGER.warn("Player {} tried to change terminal override without admin permission",
                        serverPlayer.getName().getString());
                // 必须回执：客户端的输入框已经乐观改成了新值，不回执就永久显示一个没生效的覆盖。
                reject(serverPlayer, "无管理员权限");
                return;
            }

            if (!Double.isFinite(payload.delta())) {
                PiranPort.LOGGER.warn("Rejected non-finite terminal override: {} / {}",
                        payload.category(), payload.key());
                reject(serverPlayer, "偏移量非法");
                return;
            }

            ServerLevel level = serverPlayer.serverLevel();
            TerminalOverridesSavedData data = TerminalOverridesSavedData.get(level);

            switch (payload.category()) {
                case CATEGORY_TORPEDO -> {
                    // 裸注册 ID（无 piranport: 前缀）是 modelKey 的约定形式，
                    // 但终端 UI 拿到的可能是带命名空间的串，这里统一归一化后再校验。
                    String modelKey = normalizeTorpedoKey(payload.key());
                    if (modelKey == null) {
                        PiranPort.LOGGER.warn("Rejected unknown torpedo model key: {}", payload.key());
                        reject(serverPlayer, "未知鱼雷型号: " + payload.key());
                        return;
                    }
                    data.setTorpedoSpeedDelta(modelKey, TerminalOverridesSavedData.clampTorpedoDelta((float) payload.delta()));
                    // 鱼雷是实体化的，后续发射会读取新速度；已飞行的鱼雷不受影响（设计如此，见终端 UI 说明）。
                }
                case CATEGORY_CORE -> {
                    // 与鱼雷分支对齐：键必须是合法 ShipType 枚举名（SMALL/MEDIUM/LARGE/SUBMARINE）。
                    // 不走白名单的话，非法键会永久写进存档且永远匹配不到读取方 activeType.name()。
                    String coreKey = TerminalOverridesSavedData.normalizeCoreKey(payload.key());
                    if (coreKey == null) {
                        PiranPort.LOGGER.warn("Rejected unknown ship type key: {}", payload.key());
                        reject(serverPlayer, "未知舰型: " + payload.key());
                        return;
                    }
                    // 不要把 delta 在调用点先钳一遍：core 的钳制下限是 0.1，先钳会把「填 0 清除覆盖」
                    // 变成 +0.1。让 setCoreSpeedDelta 自己判 0 走移除、其余才钳。
                    data.setCoreSpeedDelta(coreKey, payload.delta());
                    // R1：核心航速走的是玩家属性修饰符，写覆盖不会自动重算，
                    // 必须显式重放，否则只有下次变形/换装才生效。
                    com.piranport.combat.TransformationManager.onTerminalCoreOverrideChanged(serverPlayer);
                }
                default -> {
                    PiranPort.LOGGER.warn("Unknown terminal override category: {}", payload.category());
                    reject(serverPlayer, "未知分类: " + payload.category());
                    return;
                }
            }

            // 回推最新快照：客户端镜像只在收到 sync 包时更新，而写覆盖的客户端
            // 不刷新镜像的话，切页/滚动/缩放触发 rebuildWidgets 会把输入框回退成旧值
            // （单机因客户端与服务端同 JVM 共享静态镜像而看不出来，联机才暴露）。
            PacketDistributor.sendToPlayer(serverPlayer, SyncTerminalOverridesPayload.from(data));

            if (payload.category().equals(CATEGORY_CORE)) {
                PiranPort.LOGGER.info("Player {} set core speed delta {} = {}",
                        serverPlayer.getName().getString(), payload.key(), payload.delta());
            } else {
                PiranPort.LOGGER.info("Player {} set torpedo speed delta {} = {}",
                        serverPlayer.getName().getString(), payload.key(), payload.delta());
            }
        });
    }

    /**
     * 拒绝回执：告诉客户端本次修改没有生效。
     *
     * <p>WHY 不只是 LOGGER.warn：终端界面在发包时就把输入框改成了新值，静默拒绝会让界面
     * 一直显示一个存档里不存在的覆盖值，直到玩家重启客户端——比直接报错更难查。
     */
    private static void reject(ServerPlayer player, String reason) {
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "§c[PP] 速度覆盖未生效：" + reason), false);
    }

    /**
     * 把玩家/UI 传来的鱼雷标识归一化成裸注册 ID，并校验它确实是已注册的鱼雷。
     *
     * <p>WHY 必须校验：modelKey 是纯字符串，写进去的垃圾键会永久留在存档里，
     * 每次读覆盖都要白查一次 Map。这里在入口挡掉，存档里就只会出现真实存在的型号。
     *
     * <p>可见性为包级：重置路径 {@link ResetTerminalOverridesPayload} 也要用它
     * 把单键重置的键归一化到与写入一致的键空间。
     *
     * @return 归一化后的裸注册 ID；不是已注册鱼雷时返回 null
     */
    static String normalizeTorpedoKey(String rawKey) {
        if (rawKey == null || rawKey.isEmpty()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(rawKey);
        if (id == null) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        if (!(item instanceof TorpedoItem torpedo)) {
            return null;
        }
        String modelKey = torpedo.getModelKey();
        // 没有 modelKey 的鱼雷（旧实例/未接入注册点的型号）拒绝覆盖：
        // 写进去也没有读取方，只会制造「设了没反应」的困惑。
        return (modelKey == null || modelKey.isEmpty()) ? null : modelKey;
    }
}
