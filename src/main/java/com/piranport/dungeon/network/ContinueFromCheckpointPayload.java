package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import com.piranport.dungeon.block.DungeonLecternBlock;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.key.DungeonKeyItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * C2S: Player chose "继续" — 跳转到该玩家的最新记录点位置。
 *
 * <p>整合版 §3.1/§3.2：玩家个人最新记录点用于"继续"按钮，传送玩家到对应坐标。</p>
 */
public record ContinueFromCheckpointPayload(BlockPos lecternPos)
        implements CustomPacketPayload {

    public static final Type<ContinueFromCheckpointPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "continue_from_checkpoint"));

    public static final StreamCodec<ByteBuf, ContinueFromCheckpointPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> buf.writeLong(p.lecternPos().asLong()),
            buf -> new ContinueFromCheckpointPayload(BlockPos.of(buf.readLong()))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ContinueFromCheckpointPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            BlockPos lecternPos = payload.lecternPos();
            if (!(player.level().getBlockState(lecternPos).getBlock() instanceof DungeonLecternBlock)) return;
            if (player.distanceToSqr(lecternPos.getX() + 0.5,
                    lecternPos.getY() + 0.5,
                    lecternPos.getZ() + 0.5) > 64.0) return;

            BlockEntity be = player.level().getBlockEntity(lecternPos);
            if (!(be instanceof com.piranport.dungeon.block.DungeonLecternBlockEntity lecternBE)) return;
            if (!lecternBE.hasKey()) return;

            // 整合版 §3.2：拉玩家最新 checkpoint
            ServerLevel serverLevel = (ServerLevel) player.level();
            DungeonInstanceManager mgr = DungeonInstanceManager.get(serverLevel);
            UUID instanceId = lecternBE.getDungeonInstanceUuid();
            ItemStack keyStack = lecternBE.getKeyStack();

            DungeonInstance instance = null;
            if (instanceId != null) {
                instance = mgr.getInstance(instanceId);
            }
            if (instance == null) {
                // 新副本：尚未创建实例——使用钥匙上的 stageId 创建并直接进入起点
                if (keyStack.isEmpty()) return;
                String stageId = DungeonKeyItem.getStageId(keyStack);
                com.piranport.dungeon.data.StageData stage =
                        com.piranport.dungeon.data.DungeonRegistry.INSTANCE.getStage(stageId);
                if (stage == null) return;
                String startNode = stage.startNode();
                if (startNode == null) return;
                instance = mgr.createInstance(stageId, player,
                        lecternPos,
                        player.level().dimension().location().toString());
                if (instance == null) return;
                com.piranport.dungeon.event.DungeonEventHandler.enterNode(
                        serverLevel, instance,
                        stage.nodes().get(startNode),
                        stage, player, keyStack);
                return;
            }

            String cpId = instance.getLatestCheckpointFor(player.getUUID());
            if (cpId == null) return;
            StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
            if (stage == null) return;
            com.piranport.dungeon.data.CheckpointData cp = stage.checkpoints().stream()
                    .filter(c -> c.id().equals(cpId)).findFirst().orElse(null);
            if (cp == null) return;

            // 传送到 checkpoint 坐标（仅本地维度，不跨维度）
            player.teleportTo(serverLevel,
                    cp.posX() + 0.5, cp.posY() + 0.1, cp.posZ() + 0.5,
                    player.getYRot(), player.getXRot());
        });
    }
}