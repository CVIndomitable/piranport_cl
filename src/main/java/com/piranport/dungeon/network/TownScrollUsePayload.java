package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import com.piranport.dungeon.event.DungeonEventHandler;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.key.FlagshipManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S: Player confirms using the town scroll to leave the dungeon.
 */
public record TownScrollUsePayload() implements CustomPacketPayload {

    public static final Type<TownScrollUsePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "town_scroll_use"));

    public static final StreamCodec<ByteBuf, TownScrollUsePayload> STREAM_CODEC =
            StreamCodec.unit(new TownScrollUsePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(TownScrollUsePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            // Find and consume town scroll
            Inventory inv = player.getInventory();
            boolean found = false;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.getItem() instanceof com.piranport.dungeon.item.TownScrollItem) {
                    stack.shrink(1);
                    found = true;
                    break;
                }
            }
            if (!found) return;

            // Find active instance via key
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.getItem() instanceof DungeonKeyItem) {
                    java.util.UUID instanceId = DungeonKeyItem.getInstanceId(stack);
                    if (instanceId == null) continue;

                    DungeonInstanceManager mgr = DungeonInstanceManager.get(
                            (ServerLevel) player.level());
                    DungeonInstance instance = mgr.getInstance(instanceId);
                    if (instance == null) continue;

                    // Teleport back to lectern
                    DungeonEventHandler.teleportToLectern(player, instance);

                    // Check if dungeon is now empty
                    DungeonEventHandler.checkAndSuspendIfEmpty(player.server, instance);
                    return;
                }
            }
        });
    }
}
