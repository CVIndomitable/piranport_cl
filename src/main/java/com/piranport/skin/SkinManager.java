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

    /** 恢复皮肤并将存储的核心返还到背包（用于空手恢复）。 */
    public static void revertSkin(ServerPlayer player) {
        int currentSkin = getActiveSkin(player);
        if (currentSkin <= 0) return;
        returnSkinCore(player, currentSkin);
        clearActiveSkin(player);
    }

    /** 给玩家一个对应 skinId 的皮肤核心物品。 */
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
            case 4 -> new ItemStack(ModItems.SKIN_CORE_4.get());
            case 5 -> new ItemStack(ModItems.SKIN_CORE_5.get());
            case 6 -> new ItemStack(ModItems.SKIN_CORE_6.get());
            case 7 -> new ItemStack(ModItems.SKIN_CORE_7.get());
            case 8 -> new ItemStack(ModItems.SKIN_CORE_8.get());
            case 9 -> new ItemStack(ModItems.SKIN_CORE_9.get());
            case 10 -> new ItemStack(ModItems.SKIN_CORE_10.get());
            case 11 -> new ItemStack(ModItems.SKIN_CORE_11.get());
            case 12 -> new ItemStack(ModItems.SKIN_CORE_12.get());
            case 13 -> new ItemStack(ModItems.SKIN_CORE_13.get());
            case 14 -> new ItemStack(ModItems.SKIN_CORE_14.get());
            case 15 -> new ItemStack(ModItems.SKIN_CORE_15.get());
            case 16 -> new ItemStack(ModItems.SKIN_CORE_16.get());
            case 17 -> new ItemStack(ModItems.SKIN_CORE_17.get());
            case 18 -> new ItemStack(ModItems.SKIN_CORE_18.get());
            case 19 -> new ItemStack(ModItems.SKIN_CORE_19.get());
            case 20 -> new ItemStack(ModItems.SKIN_CORE_20.get());
            case 21 -> new ItemStack(ModItems.SKIN_CORE_21.get());
            case 22 -> new ItemStack(ModItems.SKIN_CORE_22.get());
            default -> ItemStack.EMPTY;
        };
    }

    /** 向刚加入的玩家发送所有当前激活的皮肤状态。 */
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
