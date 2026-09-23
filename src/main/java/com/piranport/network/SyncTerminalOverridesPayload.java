package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.terminal.TerminalOverrides;
import com.piranport.terminal.TerminalOverridesSavedData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

/**
 * 同步调试终端覆盖网络包（服务端 → 客户端）。
 *
 * <p>终端打开时发送（同时作为每次修改后的回应广播），把当前存档的覆盖值刷到客户端。
 *
 * <p>WHY 不直接复用运行时镜像 {@link TerminalOverrides}：客户端镜像只在连接上服务端后
 * 才会被同步，而单机玩家从主菜单进存档的瞬间镜像还是空的；终端界面打开时主动拉一次，
 * 显示的就是权威值而不是本地猜测。
 */
public record SyncTerminalOverridesPayload(
        Map<String, Float> torpedoDeltas,  // 型号注册 ID → 航速偏移（blocks/tick）
        Map<String, Double> coreDeltas     // 舰型名 → 航速倍率偏移
) implements CustomPacketPayload {

    public static final Type<SyncTerminalOverridesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "sync_terminal_overrides"));

    /** 上限与火炮域同量级：型号数 + 舰型数各几十，留足冗余但不允许畸形包无限膨胀。 */
    private static final int MAX_ENTRIES = 512;
    private static final int MAX_KEY_LENGTH = 128;

    public static final StreamCodec<ByteBuf, SyncTerminalOverridesPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                ByteBufCodecs.VAR_INT.encode(buffer, payload.torpedoDeltas().size());
                for (Map.Entry<String, Float> entry : payload.torpedoDeltas().entrySet()) {
                    ByteBufCodecs.stringUtf8(MAX_KEY_LENGTH).encode(buffer, entry.getKey());
                    ByteBufCodecs.FLOAT.encode(buffer, entry.getValue());
                }

                ByteBufCodecs.VAR_INT.encode(buffer, payload.coreDeltas().size());
                for (Map.Entry<String, Double> entry : payload.coreDeltas().entrySet()) {
                    ByteBufCodecs.stringUtf8(MAX_KEY_LENGTH).encode(buffer, entry.getKey());
                    ByteBufCodecs.DOUBLE.encode(buffer, entry.getValue());
                }
            },
            buffer -> {
                Map<String, Float> torpedoes = new HashMap<>();
                int torpedoCount = ByteBufCodecs.VAR_INT.decode(buffer);
                for (int i = 0; i < torpedoCount; i++) {
                    String key = ByteBufCodecs.stringUtf8(MAX_KEY_LENGTH).decode(buffer);
                    torpedoes.put(key, ByteBufCodecs.FLOAT.decode(buffer));
                }

                Map<String, Double> cores = new HashMap<>();
                int coreCount = ByteBufCodecs.VAR_INT.decode(buffer);
                for (int i = 0; i < coreCount; i++) {
                    String key = ByteBufCodecs.stringUtf8(MAX_KEY_LENGTH).decode(buffer);
                    cores.put(key, ByteBufCodecs.DOUBLE.decode(buffer));
                }

                return new SyncTerminalOverridesPayload(torpedoes, cores);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 服务端侧构造：直接从 SavedData 抓一份快照。 */
    public static SyncTerminalOverridesPayload from(TerminalOverridesSavedData data) {
        return new SyncTerminalOverridesPayload(data.getAllTorpedoDeltas(), data.getAllCoreDeltas());
    }

    /**
     * 客户端处理：只更新本地镜像，供终端界面读取。
     *
     * <p>不改动任何实体状态 —— 服务端才是速度的唯一来源，
     * 客户端镜像只服务于「界面显示当前覆盖值」这一件事。
     */
    public static void handle(SyncTerminalOverridesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 解码侧已按条目建好 Map，直接整份替换，避免新旧覆盖混合出幽灵值。
            TerminalOverrides.apply(new HashMap<>(payload.torpedoDeltas()), new HashMap<>(payload.coreDeltas()));
        });
    }
}
