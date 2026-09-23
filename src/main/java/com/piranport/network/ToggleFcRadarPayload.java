package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.combat.TransformationManager;
import com.piranport.registry.ModDataComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S：切换火控雷达开关（0 键）。
 *
 * <p>WHY 状态由服务端翻转而不是客户端：{@link ModDataComponents#SHIP_FC_RADAR_ON}
 * 是写在舰装核心上的持久状态，客户端只负责发「按下了一次 0 键」这个意图。
 * 若让客户端自己翻转再上报，作弊客户端可以本地开启吸附却不上报，导致
 * 「本地准星在吸、服务端认为没开」的判定分歧。服务端翻转 + DataComponent
 * 自动回同步，客户端无需自己维护镜像状态。
 *
 * <p>同样地，服务端会二次校验核心强化槽里是否真的装了火控雷达 —— 键位可以被
 * 任何客户端伪造，装备存在性不能信客户端。
 */
public record ToggleFcRadarPayload() implements CustomPacketPayload {

    public static final Type<ToggleFcRadarPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "toggle_fc_radar"));

    public static final StreamCodec<ByteBuf, ToggleFcRadarPayload> STREAM_CODEC =
            StreamCodec.unit(new ToggleFcRadarPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleFcRadarPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;

            ItemStack coreStack = TransformationManager.findTransformedCore(player);
            if (coreStack.isEmpty()) return;

            // 未装备则直接拒绝 —— 键位可被客户端伪造，装备存在性必须服务端复核
            if (!TransformationManager.hasFireControlRadarEquipped(player, coreStack)) return;

            boolean next = !Boolean.TRUE.equals(coreStack.get(ModDataComponents.SHIP_FC_RADAR_ON.get()));
            coreStack.set(ModDataComponents.SHIP_FC_RADAR_ON.get(), next);
            TransformationManager.writeCoreToConfiguredSlot(player, coreStack);

            player.displayClientMessage(Component.translatable(
                    next ? "message.piranport.fire_control_radar_on"
                         : "message.piranport.fire_control_radar_off"
            ), true);

            // 顺手把「模拟距离换算出的吸附上限」下发给客户端。
            //
            // WHY 必须服务端算：模拟距离只存在于服务端（MinecraftServer#getPlayerList），
            // 而吸附逻辑跑在客户端（要让准星每帧跟上，走服务端会有明显延迟）。
            // 客户端自己去取会触发 ArchitectureTest 的 client→server 依赖禁令，集成服里也拿不到。
            // 挂在开关包上是因为玩家必须先按 0 键开启才会吸附，这一步天然保证下发先于使用。
            if (player instanceof ServerPlayer serverPlayer) {
                int simDist = serverPlayer.server.getPlayerList().getSimulationDistance();
                FcRangeSyncPayload.send(serverPlayer, simDist * 16.0 - 8.0);
            }
        });
    }
}
