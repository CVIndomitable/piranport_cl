package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.combat.TransformationManager;
import com.piranport.registry.ModDataComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S：请求服务端重新下发火控雷达的吸附半径上限（格）。
 *
 * <p>WHY 需要这个包：{@link FcRangeSyncPayload} 原先只挂在「按 0 键开关雷达」上，
 * 那一步天然保证「下发先于使用」。但客户端的半径缓存是<b>会话内</b>的静态字段，
 * 而死亡重生与跨维度会让玩家实体整个换一个新的（客户端收到
 * {@code ClientPlayerNetworkEvent.Clone}），此时必须把缓存清掉 —— 否则换维度后
 * 「上一个维度的模拟距离」会跟着走，吸到本维度根本没加载的实体上。
 * 清掉之后如果没有补发路径，处于「雷达一直是开着」状态的玩家就会静默失效：
 * 组件仍是 {@code true}（不会触发按 0 的开关分支，也就不会下发），
 * 而吸附半径缓存是 0，{@code collectCandidates} 的 range 上限直接归零。
 *
 * <p>所以客户端在清缓存之后<b>主动来要一次</b>，把「缓存有效」这件事重新建立起来，
 * 而不是依赖玩家碰巧去按 0 键。服务端仍然自行复核装备存在性与开关状态 ——
 * 客户端只能发「我想要一份半径」，不能自己编一个半径出来。
 */
public record FcRangeRequestPayload() implements CustomPacketPayload {

    public static final Type<FcRangeRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "fc_range_request"));

    public static final StreamCodec<ByteBuf, FcRangeRequestPayload> STREAM_CODEC =
            StreamCodec.unit(new FcRangeRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FcRangeRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;

            // 只有真的「装备了雷达且开着」才回发。这一层复核的意义不在于防作弊
            // （半径本身不是敏感信息），而在于避免给一个压根没用雷达的玩家白发一个包。
            ItemStack coreStack = TransformationManager.findTransformedCore(player);
            if (coreStack.isEmpty()) return;
            if (!TransformationManager.hasFireControlRadarEquipped(player, coreStack)) return;
            if (!Boolean.TRUE.equals(coreStack.get(ModDataComponents.SHIP_FC_RADAR_ON.get()))) return;

            if (player instanceof ServerPlayer serverPlayer) {
                int simDist = serverPlayer.server.getPlayerList().getSimulationDistance();
                FcRangeSyncPayload.send(serverPlayer, simDist * 16.0 - 8.0);
            }
        });
    }
}
