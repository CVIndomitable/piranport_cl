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
 * C2S: Player chose "从头开始" — 跳转到副本起点（stage.startNode 节点）。
 *
 * <p>整合版 §3.1：怪物/宝箱不刷新（已击杀不复活、已开启不重置）——目的是允许玩家寻找遗漏的宝箱。
 * 传送玩家到副本起点对应的 node spawn 位置。</p>
 */
public record RestartFromBeginningPayload(BlockPos lecternPos)
        implements CustomPacketPayload {

    public static final Type<RestartFromBeginningPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "restart_from_beginning"));

    public static final StreamCodec<ByteBuf, RestartFromBeginningPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> buf.writeLong(p.lecternPos().asLong()),
            buf -> new RestartFromBeginningPayload(BlockPos.of(buf.readLong()))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(RestartFromBeginningPayload payload, IPayloadContext context) {
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

            StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
            if (stage == null) return;

            // 整合版 §3.1：起点固定（lectern 维度）；从讲台 BE 位置开始
            BlockPos startPos = lecternPos;
            player.teleportTo(serverLevel,
                    startPos.getX() + 0.5, startPos.getY() + 1.0, startPos.getZ() + 0.5,
                    player.getYRot(), player.getXRot());
        });
    }
}