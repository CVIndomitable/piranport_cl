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

            // Only usable inside the dungeon dimension
            if (!player.level().dimension().equals(
                    com.piranport.dungeon.event.DungeonEventHandler.DUNGEON_DIMENSION)) return;

            // Require a recent server-side right-click intent so a hand-crafted
            // payload from a tampered client cannot trigger the teleport on its own.
            com.piranport.dungeon.item.TownScrollItem.Intent intent =
                    com.piranport.dungeon.item.TownScrollItem.consumeIntent(player.getUUID());
            if (intent == null) return;

            Inventory inv = player.getInventory();

            // Pre-check: player must hold an active dungeon instance via key, otherwise
            // refund the intent (no consume) so the totem-equivalent isn't burned.
            DungeonInstanceManager mgr = DungeonInstanceManager.get((ServerLevel) player.level());
            DungeonInstance targetInstance = null;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.getItem() instanceof DungeonKeyItem) {
                    java.util.UUID instanceId = DungeonKeyItem.getInstanceId(stack);
                    if (instanceId == null) continue;
                    DungeonInstance inst = mgr.getInstance(instanceId);
                    if (inst != null) {
                        targetInstance = inst;
                        break;
                    }
                }
            }
            if (targetInstance == null) return;

            // Try to consume the exact slot the player right-clicked. Fall back to
            // any TownScrollItem in inventory if the slot moved (e.g. drag).
            int slot = intent.slot();
            ItemStack chosen = (slot >= 0 && slot < inv.getContainerSize())
                    ? inv.getItem(slot) : ItemStack.EMPTY;
            if (!(chosen.getItem() instanceof com.piranport.dungeon.item.TownScrollItem)) {
                int fallback = -1;
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    if (inv.getItem(i).getItem()
                            instanceof com.piranport.dungeon.item.TownScrollItem) {
                        fallback = i;
                        break;
                    }
                }
                if (fallback < 0) return;
                chosen = inv.getItem(fallback);
            }

            chosen.shrink(1);

            DungeonEventHandler.teleportToLectern(player, targetInstance);
            DungeonEventHandler.checkAndSuspendIfEmpty(player.server, targetInstance);
        });
    }
}
