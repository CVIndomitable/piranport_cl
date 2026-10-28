package com.piranport.entitycore;

import com.piranport.network.EntityCoreSyncPayload;
import com.piranport.registry.ModAttachmentTypes;
import com.piranport.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class EntityCoreState {
    public static int getActiveEntityCore(Player player) {
        return player.getData(ModAttachmentTypes.ACTIVE_ENTITY_CORE.get());
    }

    public static void setActiveEntityCore(ServerPlayer player, int coreId) {
        if (!EntityCoreDefinitions.isValid(coreId)) return;
        player.setData(ModAttachmentTypes.ACTIVE_ENTITY_CORE.get(), coreId);
        PacketDistributor.sendToAllPlayers(new EntityCoreSyncPayload(player.getUUID(), coreId));
    }

    public static void clearActiveEntityCore(ServerPlayer player) {
        player.setData(ModAttachmentTypes.ACTIVE_ENTITY_CORE.get(), 0);
        PacketDistributor.sendToAllPlayers(new EntityCoreSyncPayload(player.getUUID(), 0));
    }

    public static void revertEntityCore(ServerPlayer player) {
        int currentCore = getActiveEntityCore(player);
        if (currentCore <= 0) return;
        returnEntityCore(player, currentCore);
        clearActiveEntityCore(player);
    }

    public static void returnEntityCore(ServerPlayer player, int coreId) {
        ItemStack coreItem = getEntityCoreItem(coreId);
        if (!coreItem.isEmpty()) {
            if (!player.getInventory().add(coreItem)) {
                player.drop(coreItem, false);
            }
        }
    }

    public static ItemStack getEntityCoreItem(int coreId) {
        // 只有平海核心仍是物品；其余 coreId 的变身定义保留，但复原时不再返还物品
        // 返回 EMPTY 而非抛异常，保证旧存档里已激活的深海核心可以正常复原
        return switch (coreId) {
            case EntityCoreDefinitions.PINGHAI ->
                    new ItemStack(ModItems.ENTITY_CORE_PINGHAI.get());
            default -> ItemStack.EMPTY;
        };
    }

    public static void syncAllEntityCoresToPlayer(ServerPlayer joiner) {
        for (ServerPlayer other : joiner.server.getPlayerList().getPlayers()) {
            int coreId = getActiveEntityCore(other);
            if (coreId > 0) {
                PacketDistributor.sendToPlayer(joiner,
                        new EntityCoreSyncPayload(other.getUUID(), coreId));
            }
        }
    }
}
