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
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S: Player requests to revive (costs one totem of undying).
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

            // Locate the active dungeon instance + spawn target FIRST. Only after we
            // know the revive can succeed do we consume the totem of undying.
            DungeonInstance targetInstance = null;
            String targetNode = null;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.getItem() instanceof com.piranport.dungeon.key.DungeonKeyItem) {
                    java.util.UUID instanceId =
                            com.piranport.dungeon.key.DungeonKeyItem.getInstanceId(stack);
                    if (instanceId == null) continue;
                    DungeonInstance instance = mgr.getInstance(instanceId);
                    if (instance == null || instance.getState() != DungeonInstance.State.ACTIVE) continue;
                    String nodeId = instance.getCurrentNode();
                    if (nodeId == null) continue;
                    targetInstance = instance;
                    targetNode = nodeId;
                    break;
                }
            }

            ServerLevel dungeonLevel = com.piranport.dungeon.event.DungeonEventHandler
                    .getDungeonLevel(player.server);
            if (targetInstance == null || dungeonLevel == null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "dungeon.piranport.revive_unavailable"));
                // Re-open the revive screen so the player can choose to give up
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
}
