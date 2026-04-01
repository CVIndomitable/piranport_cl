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

            // Find and consume totem
            Inventory inv = player.getInventory();
            boolean found = false;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.is(Items.TOTEM_OF_UNDYING)) {
                    stack.shrink(1);
                    found = true;
                    break;
                }
            }
            if (!found) return;

            // Find the player's active dungeon instance and teleport to node spawn
            // For now, we look through all instances for one containing this player
            DungeonInstanceManager mgr = DungeonInstanceManager.get((ServerLevel) player.level());
            // Player should have a tag or we iterate — use a simple approach
            // The player was teleported back to the lectern on death, so they're in overworld now
            // We need to find their instance and teleport them back to the battlefield
            ServerLevel overworld = player.server.overworld();
            // Search through instances (not ideal but workable for now)
            // A better approach would store the instance ID on the player
            // For now, check key in inventory
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.getItem() instanceof com.piranport.dungeon.key.DungeonKeyItem) {
                    java.util.UUID instanceId = com.piranport.dungeon.key.DungeonKeyItem.getInstanceId(stack);
                    if (instanceId == null) continue;
                    DungeonInstance instance = mgr.getInstance(instanceId);
                    if (instance == null || instance.getState() != DungeonInstance.State.ACTIVE) continue;

                    String nodeId = instance.getCurrentNode();
                    if (nodeId == null) continue;

                    BlockPos spawn = instance.getNodeSpawnPos(nodeId);
                    // Get dungeon dimension
                    ServerLevel dungeonLevel = com.piranport.dungeon.event.DungeonEventHandler
                            .getDungeonLevel(player.server);
                    if (dungeonLevel != null) {
                        player.teleportTo(dungeonLevel, spawn.getX() + 0.5, spawn.getY(),
                                spawn.getZ() + 0.5, player.getYRot(), player.getXRot());
                    }
                    return;
                }
            }
        });
    }
}
