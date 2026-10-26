package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.aviation.FireControlManager;
import com.piranport.platform.ClientHooks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record FireControlSyncPayload(List<UUID> targetUUIDs) implements CustomPacketPayload {

    public static final Type<FireControlSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "fire_control_sync"));

    public static final StreamCodec<ByteBuf, FireControlSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.VAR_INT.encode(buf, p.targetUUIDs().size());
                for (UUID uuid : p.targetUUIDs()) {
                    buf.writeLong(uuid.getMostSignificantBits());
                    buf.writeLong(uuid.getLeastSignificantBits());
                }
            },
            buf -> {
                int size = ByteBufCodecs.VAR_INT.decode(buf);
                if (size < 0 || size > 16) size = 0; // guard against oversized packets
                List<UUID> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(new UUID(buf.readLong(), buf.readLong()));
                }
                return new FireControlSyncPayload(List.copyOf(list));
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /**
     * 把玩家当前的服务端火控列表整份推给客户端。
     *
     * <p>WHY 需要这个统一入口：服务端在多个路径（死亡清理、召回舰载机、换维度、
     * 手动取消）都会改动 {@link FireControlManager}。只要有一条路径改了列表却不发包，
     * 客户端就会一直留着旧目标，表现为「目标已经没了，HUD 还挂着」。所有改动点
     * 都走这里，避免再次漏同步。</p>
     */
    public static void syncTo(ServerPlayer player) {
        if (player == null || player.hasDisconnected()) return;
        PacketDistributor.sendToPlayer(player, new FireControlSyncPayload(
                FireControlManager.getTargets(player.getUUID())));
    }

    /** 对可能不是服务端玩家的调用方提供空安全版本（客户端侧或已断线时静默跳过）。 */
    public static void syncIfServerPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) syncTo(serverPlayer);
    }

    // ===== Client-side handler =====

    public static void handle(FireControlSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientHooks.setFireControlTargets(payload.targetUUIDs()));
    }
}
