package com.piranport.skin;

import com.piranport.network.SkinSyncPayload;
import com.piranport.registry.ModAttachmentTypes;
import com.piranport.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class SkinManager {

    public static int getActiveSkin(Player player) {
        return player.getData(ModAttachmentTypes.ACTIVE_SKIN.get());
    }

    public static void setActiveSkin(ServerPlayer player, int skinId) {
        player.setData(ModAttachmentTypes.ACTIVE_SKIN.get(), skinId);
        PacketDistributor.sendToAllPlayers(new SkinSyncPayload(player.getUUID(), skinId));
    }

    public static void clearActiveSkin(ServerPlayer player) {
        player.setData(ModAttachmentTypes.ACTIVE_SKIN.get(), 0);
        PacketDistributor.sendToAllPlayers(new SkinSyncPayload(player.getUUID(), 0));
    }

    /** Revert skin and return the stored core to inventory (used by empty-hand revert). */
    public static void revertSkin(ServerPlayer player) {
        int currentSkin = getActiveSkin(player);
        if (currentSkin <= 0) return;
        returnSkinCore(player, currentSkin);
        clearActiveSkin(player);
    }

    /** Give the player a skin core item corresponding to the given skinId. */
    public static void returnSkinCore(ServerPlayer player, int skinId) {
        ItemStack coreItem = getSkinCoreItem(skinId);
        if (!coreItem.isEmpty()) {
            if (!player.getInventory().add(coreItem)) {
                player.drop(coreItem, false);
            }
        }
    }

    public static ItemStack getSkinCoreItem(int skinId) {
        return switch (skinId) {
            case 1 -> new ItemStack(ModItems.SKIN_CORE_1.get());
            case 2 -> new ItemStack(ModItems.SKIN_CORE_2.get());
            case 3 -> new ItemStack(ModItems.SKIN_CORE_3.get());
            default -> ItemStack.EMPTY;
        };
    }

    /** Send all currently active skins to a player who just joined. */
    public static void syncAllSkinsToPlayer(ServerPlayer joiner) {
        for (ServerPlayer other : joiner.server.getPlayerList().getPlayers()) {
            int skinId = getActiveSkin(other);
            if (skinId > 0) {
                PacketDistributor.sendToPlayer(joiner,
                        new SkinSyncPayload(other.getUUID(), skinId));
            }
        }
    }
}
