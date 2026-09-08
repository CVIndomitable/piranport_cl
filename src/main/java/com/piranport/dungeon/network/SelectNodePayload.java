package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.block.DungeonLecternBlock;
import com.piranport.dungeon.event.DungeonEventHandler;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.registry.ModDataComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * C2S: Player selects a node to enter in the node map.
 *
 * <p>整合版 §3.1 联机大厅已作废——任何持有同副本钥匙的玩家均可推进节点（无队长/无旗舰校验）。
 * 多玩家时通过 instance.playerUuids 拉取所有参与玩家进入同一节点。</p>
 *
 * <p>服务端共用入口 {@link #serverSideHandle}：既被本 payload 的 {@code handle} 调用，
 * 也被 {@link DungeonLecternBlock} 服务端直接调用（钥匙在讲台 BE 上时）。</p>
 */
public record SelectNodePayload(BlockPos lecternPos, int keySlot, String nodeId)
        implements CustomPacketPayload {

    private static final int MAX_ID_LENGTH = 128;

    public static final Type<SelectNodePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "select_node"));

    public static final StreamCodec<ByteBuf, SelectNodePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeLong(p.lecternPos().asLong());
                ByteBufCodecs.VAR_INT.encode(buf, p.keySlot());
                ByteBufCodecs.STRING_UTF8.encode(buf, p.nodeId());
            },
            buf -> new SelectNodePayload(
                    BlockPos.of(buf.readLong()),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.stringUtf8(MAX_ID_LENGTH).decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SelectNodePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            int keySlot = payload.keySlot();
            if (keySlot < 0 || keySlot >= player.getInventory().getContainerSize()) return;

            if (payload.nodeId().length() > 128) return;

            BlockPos lecternPos = payload.lecternPos();
            // Validate distance to lectern
            if (player.distanceToSqr(lecternPos.getX() + 0.5,
                    lecternPos.getY() + 0.5,
                    lecternPos.getZ() + 0.5) > 64.0) return;
            if (!(player.level().getBlockState(lecternPos).getBlock() instanceof DungeonLecternBlock)) return;

            ItemStack keyStack = player.getInventory().getItem(keySlot);
            if (!(keyStack.getItem() instanceof DungeonKeyItem)) return;

            UUID instanceIdHint = DungeonKeyItem.getInstanceId(keyStack);
            serverSideHandle(player, lecternPos, keyStack, instanceIdHint, payload.nodeId());
        });
    }

    /**
     * 服务端核心处理：校验节点可达性、获取/创建副本实例、推进节点。
     *
     * <p>调用方：</p>
     * <ul>
     *   <li>{@link #handle}：从玩家背包 keySlot 读取 keyStack</li>
     *   <li>{@link DungeonLecternBlock#useWithoutItem}：从讲台 BE 读取 keyStack</li>
     * </ul>
     *
     * @param player          进入副本的玩家
     * @param lecternPos      讲台方块坐标
     * @param keyStack        钥匙 ItemStack（讲台 BE 或玩家背包持有）
     * @param instanceIdHint  keyStack 上的 instanceId（可空，表示新副本）
     * @param nodeId          要进入的节点 ID
     */
    public static void serverSideHandle(ServerPlayer player, BlockPos lecternPos,
                                          ItemStack keyStack, UUID instanceIdHint,
                                          String nodeId) {
        GlobalPos globalPos = GlobalPos.of(player.level().dimension(), lecternPos);

        String stageId = DungeonKeyItem.getStageId(keyStack);
        StageData stage = DungeonRegistry.INSTANCE.getStage(stageId);
        if (stage == null) return;

        NodeData node = stage.nodes().get(nodeId);
        if (node == null) return;

        ServerLevel serverLevel = (ServerLevel) player.level();
        DungeonInstanceManager mgr = DungeonInstanceManager.get(serverLevel);
        UUID instanceId = instanceIdHint;
        DungeonInstance instance;

        if (instanceId != null) {
            instance = mgr.getInstance(instanceId);
            if (instance == null) return;
            if (!stageId.equals(instance.getStageId())) return;
            if (!matchesInstanceLectern(instance, globalPos)) return;

            // 整合版 §3.1：玩家在 instance.playerUuids 中即可推进节点（无 lobby/无旗舰权限检查）
            if (!instance.getPlayerUuids().contains(player.getUUID())) {
                instance.addPlayer(player.getUUID());
                mgr.setDirty();
            }

            if (instance.getState() == DungeonInstance.State.SUSPENDED) {
                mgr.resumeInstance(instanceId);
            }

            // Validate node reachability
            if (instance.getClearedNodes().contains(nodeId)) return;
            boolean reachable;
            if (instance.getClearedNodes().isEmpty()) {
                String startNode = stage.startNode();
                if (startNode == null || !stage.nodes().containsKey(startNode)) return;
                reachable = nodeId.equals(startNode);
            } else {
                reachable = false;
                for (String cleared : instance.getClearedNodes()) {
                    if (stage.getReachableFrom(cleared).contains(nodeId)) {
                        reachable = true;
                        break;
                    }
                }
            }
            if (!reachable) return;
        } else {
            // No instance yet — create new
            if (!nodeId.equals(stage.startNode())) return;

            instance = mgr.createInstance(stageId, player,
                    lecternPos,
                    player.level().dimension().location().toString());
            if (instance == null) return;
            // 整合版 §2.2：钥匙在讲台 BE 上，instanceId 仅写入 BE；玩家背包 key 副本不持有 instanceId
            // （若 keyStack 来自 BE，则 BE 已通过 tryInsertKey 同步持有 instanceId）
        }

        DungeonEventHandler.enterNode(serverLevel, instance, node, stage, player, keyStack);
    }

    private static boolean matchesInstanceLectern(DungeonInstance instance, GlobalPos lecternPos) {
        BlockPos instancePos = instance.getLecternPos();
        if (instancePos != null && !instancePos.equals(lecternPos.pos())) {
            return false;
        }

        String instanceDimension = instance.getLecternDimension();
        return instanceDimension == null
                || instanceDimension.equals(lecternPos.dimension().location().toString());
    }
}