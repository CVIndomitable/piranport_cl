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
        return switch (coreId) {
            case EntityCoreDefinitions.DEEP_OCEAN_SUPPLY ->
                    new ItemStack(ModItems.ENTITY_CORE_DEEP_OCEAN_SUPPLY.get());
            case EntityCoreDefinitions.DEEP_OCEAN_ARCHIVIST ->
                    new ItemStack(ModItems.ENTITY_CORE_DEEP_OCEAN_ARCHIVIST.get());
            case EntityCoreDefinitions.DEEP_OCEAN_ENGINEER ->
                    new ItemStack(ModItems.ENTITY_CORE_DEEP_OCEAN_ENGINEER.get());
            case EntityCoreDefinitions.DEEP_OCEAN_NAVIGATOR ->
                    new ItemStack(ModItems.ENTITY_CORE_DEEP_OCEAN_NAVIGATOR.get());
            case EntityCoreDefinitions.DEEP_OCEAN_QUARTERMASTER ->
                    new ItemStack(ModItems.ENTITY_CORE_DEEP_OCEAN_QUARTERMASTER.get());
            case EntityCoreDefinitions.DEEP_OCEAN_DESTROYER ->
                    new ItemStack(ModItems.ENTITY_CORE_DEEP_OCEAN_DESTROYER.get());
            case EntityCoreDefinitions.DEEP_OCEAN_LIGHT_CRUISER ->
                    new ItemStack(ModItems.ENTITY_CORE_DEEP_OCEAN_LIGHT_CRUISER.get());
            case EntityCoreDefinitions.DEEP_OCEAN_HEAVY_CRUISER ->
                    new ItemStack(ModItems.ENTITY_CORE_DEEP_OCEAN_HEAVY_CRUISER.get());
            case EntityCoreDefinitions.DEEP_OCEAN_BATTLE_CRUISER ->
                    new ItemStack(ModItems.ENTITY_CORE_DEEP_OCEAN_BATTLE_CRUISER.get());
            case EntityCoreDefinitions.DEEP_OCEAN_BATTLESHIP ->
                    new ItemStack(ModItems.ENTITY_CORE_DEEP_OCEAN_BATTLESHIP.get());
            case EntityCoreDefinitions.DEEP_OCEAN_LIGHT_CARRIER ->
                    new ItemStack(ModItems.ENTITY_CORE_DEEP_OCEAN_LIGHT_CARRIER.get());
            case EntityCoreDefinitions.DEEP_OCEAN_CARRIER ->
                    new ItemStack(ModItems.ENTITY_CORE_DEEP_OCEAN_CARRIER.get());
            case EntityCoreDefinitions.DEEP_OCEAN_SUBMARINE ->
                    new ItemStack(ModItems.ENTITY_CORE_DEEP_OCEAN_SUBMARINE.get());
            case EntityCoreDefinitions.DEEP_OCEAN_FLAGSHIP ->
                    new ItemStack(ModItems.ENTITY_CORE_DEEP_OCEAN_FLAGSHIP.get());
            case EntityCoreDefinitions.SHIP_GIRL ->
                    new ItemStack(ModItems.ENTITY_CORE_SHIP_GIRL.get());
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
