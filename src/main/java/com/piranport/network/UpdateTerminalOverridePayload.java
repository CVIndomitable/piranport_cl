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
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }

            // 只考虑单机，但权限门禁保留：局域网开放时非房主也连得上，
            // 不能因为「设计上单机」就默认每个连接都有管理员权限。
            if (!ConfigToolPermissions.canUse(serverPlayer)) {
                PiranPort.LOGGER.warn("Player {} tried to change terminal override without admin permission",
                        serverPlayer.getName().getString());
                return;
            }

            if (!Double.isFinite(payload.delta())) {
                PiranPort.LOGGER.warn("Rejected non-finite terminal override: {} / {}",
                        payload.category(), payload.key());
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
                        return;
                    }
                    data.setTorpedoSpeedDelta(modelKey, TerminalOverridesSavedData.clampTorpedoDelta((float) payload.delta()));
                    // 鱼雷是实体化的，后续发射会读取新速度；已飞行的鱼雷不受影响（设计如此，见终端 UI 说明）。
                }
                case CATEGORY_CORE -> {
                    String coreKey = payload.key();
                    if (coreKey == null || coreKey.isEmpty()) {
                        return;
                    }
                    data.setCoreSpeedDelta(coreKey, TerminalOverridesSavedData.clampCoreDelta(payload.delta()));
                    // R1：核心航速走的是玩家属性修饰符，写覆盖不会自动重算，
                    // 必须显式重放，否则只有下次变形/换装才生效。
                    com.piranport.combat.TransformationManager.onTerminalCoreOverrideChanged(serverPlayer);
                }
                default -> PiranPort.LOGGER.warn("Unknown terminal override category: {}", payload.category());
            }

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
     * 把玩家/UI 传来的鱼雷标识归一化成裸注册 ID，并校验它确实是已注册的鱼雷。
     *
     * <p>WHY 必须校验：modelKey 是纯字符串，写进去的垃圾键会永久留在存档里，
     * 每次读覆盖都要白查一次 Map。这里在入口挡掉，存档里就只会出现真实存在的型号。
     *
     * @return 归一化后的裸注册 ID；不是已注册鱼雷时返回 null
     */
    private static String normalizeTorpedoKey(String rawKey) {
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
