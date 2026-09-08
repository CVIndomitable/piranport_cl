package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * C2S: Player requests to revive (costs one totem of undying).
 *
 * <p>整合版 §2.2：钥匙插在讲台上（玩家背包无钥匙）。复活时：
 * <ol>
 *   <li>在玩家位置附近（16 格半径）找 dungeon lectern BE</li>
 *   <li>读 BE 的 keyStack → getInstanceId → 拉对应 DungeonInstance</li>
 *   <li>消耗一个 totem，传送到 instance.currentNode 的 spawn pos</li>
 * </ol>
 * </p>
 */
public record ReviveRequestPayload() implements CustomPacketPayload {

    public static final Type<ReviveRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "revive_request"));

    public static final StreamCodec<ByteBuf, ReviveRequestPayload> STREAM_CODEC =
            StreamCodec.unit(new ReviveRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ReviveRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            // Only valid when player is NOT in the dungeon (teleported out on death)
            if (player.level().dimension().equals(
                    com.piranport.dungeon.event.DungeonEventHandler.DUNGEON_DIMENSION)) return;
            if (!player.isAlive()) return;

            Inventory inv = player.getInventory();
            DungeonInstanceManager mgr = DungeonInstanceManager.get((ServerLevel) player.level());

            // 整合版 §2.2：通过附近讲台 BE 找副本钥匙（不再扫玩家背包）
            com.piranport.dungeon.block.DungeonLecternBlockEntity lecternBE = findNearbyLectern(player);
            if (lecternBE == null || !lecternBE.hasKey()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "dungeon.piranport.revive_unavailable"));
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                        new com.piranport.dungeon.network.PlayerDiedInDungeonPayload());
                return;
            }

            UUID instanceId = lecternBE.getDungeonInstanceUuid();
            if (instanceId == null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "dungeon.piranport.revive_unavailable"));
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                        new com.piranport.dungeon.network.PlayerDiedInDungeonPayload());
                return;
            }

            DungeonInstance targetInstance = mgr.getInstance(instanceId);
            if (targetInstance == null
                    || targetInstance.getState() != DungeonInstance.State.ACTIVE) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "dungeon.piranport.revive_unavailable"));
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                        new com.piranport.dungeon.network.PlayerDiedInDungeonPayload());
                return;
            }
            String targetNode = targetInstance.getCurrentNode();
            if (targetNode == null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "dungeon.piranport.revive_unavailable"));
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                        new com.piranport.dungeon.network.PlayerDiedInDungeonPayload());
                return;
            }

            ServerLevel dungeonLevel = com.piranport.dungeon.event.DungeonEventHandler
                    .getDungeonLevel(player.server);
            if (dungeonLevel == null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "dungeon.piranport.revive_unavailable"));
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                        new com.piranport.dungeon.network.PlayerDiedInDungeonPayload());
                return;
            }

            // Find a totem
            int totemSlot = -1;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (inv.getItem(i).is(Items.TOTEM_OF_UNDYING)) {
                    totemSlot = i;
                    break;
                }
            }
            if (totemSlot < 0) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "dungeon.piranport.revive_no_totem"));
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                        new com.piranport.dungeon.network.PlayerDiedInDungeonPayload());
                return;
            }

            // All preconditions met — consume totem and teleport
            inv.getItem(totemSlot).shrink(1);
            BlockPos spawn = targetInstance.getNodeSpawnPos(targetNode);
            player.teleportTo(dungeonLevel,
                    spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                    player.getYRot(), player.getXRot());
        });
    }

    /**
     * 整合版 §2.2：在玩家位置附近 16 格半径找 DungeonLecternBlockEntity。
     */
    private static com.piranport.dungeon.block.DungeonLecternBlockEntity findNearbyLectern(
            ServerPlayer player) {
        BlockPos playerPos = player.blockPosition();
        for (int dx = -16; dx <= 16; dx += 4) {
            for (int dy = -4; dy <= 4; dy += 4) {
                for (int dz = -16; dz <= 16; dz += 4) {
                    BlockPos check = playerPos.offset(dx, dy, dz);
                    if (player.level().getBlockState(check).getBlock()
                            instanceof com.piranport.dungeon.block.DungeonLecternBlock) {
                        BlockEntity be = player.level().getBlockEntity(check);
                        if (be instanceof com.piranport.dungeon.block.DungeonLecternBlockEntity lecternBE) {
                            return lecternBE;
                        }
                    }
                }
            }
        }
        return null;
    }
}