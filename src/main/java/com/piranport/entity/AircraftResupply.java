package com.piranport.entity;

import com.piranport.ammo.CreativeAmmoCache;
import com.piranport.component.AircraftState;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 飞机补给与回收 —— 从 AircraftEntity 提取的静态辅助方法。
 *
 * <p><b>设计</b>: 静态方法以 AircraftEntity 为首参数，直接读写其实例字段（同包可见性）。
 * <p><b>线程模型</b>: 服务端主线程。
 */
public class AircraftResupply {

    private AircraftResupply() {}

    // ====================================================================
    // 回收（返回物品栏 / 掉落）
    // ====================================================================

    /** @see AircraftEntity#recallAndRemove() */
    public static void recallAndRemove(AircraftEntity craft) {
        recallAndRemove(craft, false);
    }

    /** 只有飞行逻辑确认到达玩家附近时才允许生成降落视觉。 */
    static void recallAndRemove(AircraftEntity craft, boolean arrived) {
        // 返航到达与玩家主动召回可能在同一 tick 各调一次；实体一旦 discard 就不能再返还物品，
        // 否则同一架飞机会被还回两份。这里以实体是否已移除作为唯一闸口。
        if (craft.isRemoved()) return;
        com.piranport.debug.PiranPortDebug.event(
                "Aircraft RETURN | type={} entityId={} forced={}",
                craft.getAircraftClass().getSerializedName(), craft.getId(), craft.isForcedReturn());
        boolean returned = false;
        if (!craft.level().isClientSide() && craft.getOwnerUUID() != null
                && craft.level() instanceof ServerLevel sl) {
            Player owner = sl.getServer().getPlayerList().getPlayer(craft.getOwnerUUID());
            if (owner != null) {
                // Clean up recon if active
                if (craft.getFlightState() == AircraftEntity.FlightState.RECON_ACTIVE) {
                    craft.cleanupReconState(owner);
                }
                // Action bar notification
                Component aircraftName = buildReturnStack(craft).getHoverName();
                String msgKey = craft.isForcedReturn()
                        ? "message.piranport.aircraft_lost"
                        : "message.piranport.aircraft_returned";
                owner.displayClientMessage(Component.translatable(msgKey, aircraftName), true);
                int slot = craft.getWeaponSlotIndex();
                boolean landingCandidate = arrived && !craft.isAutonomous()
                        && owner.isAlive() && owner.level() == craft.level()
                        && slot >= 0 && slot < com.piranport.rigging.RiggingLoadoutResolver.HOTBAR_SIZE
                        && owner.getInventory().items.get(slot).isEmpty()
                        && craft.getOriginalStack() != null && !craft.getOriginalStack().isEmpty();
                returned = returnItemToOwner(craft, owner);
                if (returned && owner instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    com.piranport.handler.RiggingLoadoutHandler.refresh(serverPlayer);
                    if (landingCandidate) sendLandingVisual(craft, serverPlayer, sl, slot);
                }
                // 实际返还飞机时触发航空回收静默，失联强制销毁不触发。
                if (!craft.isForcedReturn() && craft.level() == owner.level()) {
                    com.piranport.combat.AASilenceManager.startSilence(owner);
                }
            } else {
                // Owner offline — still clean up recon state and chunks
                com.piranport.aviation.ReconManager.endRecon(craft.getOwnerUUID());
                craft.releaseAllForcedChunks();
                craft.setLastReturnResult("OWNER_OFFLINE");
            }
        }
        // P0-3: 实体移除埋点（带回程是否成功、存活时长）
        long lifetimeSec = (System.currentTimeMillis() - craft.spawnTimeMs()) / 1000L;
        com.piranport.debug.PiranPortDebug.aircraftRemoved(
                craft.getId(), craft.getOwnerUUID(), returned, lifetimeSec);
        craft.discard();
    }

    private static void sendLandingVisual(AircraftEntity craft, net.minecraft.server.level.ServerPlayer owner,
                                          ServerLevel level, int slot) {
        if (!owner.hasData(com.piranport.registry.ModAttachmentTypes.RIGGING_LOADOUT)) return;
        var loadout = owner.getData(com.piranport.registry.ModAttachmentTypes.RIGGING_LOADOUT.get());
        var mount = loadout.mounts().stream().filter(m -> m.sourceSlot() == slot
                && m.category() == com.piranport.rigging.RiggingCategory.AIRCRAFT
                && m.stack().is(craft.getOriginalStack().getItem())).findFirst();
        if (mount.isEmpty()) return;
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(level, null,
                owner.getX(), owner.getY(), owner.getZ(),
                Math.max(48.0, level.getServer().getPlayerList().getSimulationDistance() * 16.0),
                new com.piranport.network.AircraftLandingVisualPayload(owner.getId(), owner.getUUID(),
                        craft.getId(), craft.getUUID(), slot, mount.get().socket().ordinal(), craft.getVisualId(),
                        craft.getX(), craft.getY(), craft.getZ(), craft.getYRot(), craft.getXRot(),
                        loadout.fingerprint(), level.getGameTime(),
                        com.piranport.config.ModEquipmentConfig.AIRCRAFT_LANDING_APPROACH_TICKS.get(),
                        com.piranport.config.ModEquipmentConfig.AIRCRAFT_LANDING_ROLL_TICKS.get(),
                        com.piranport.config.ModEquipmentConfig.AIRCRAFT_LANDING_SETTLE_TICKS.get()));
    }

    static boolean returnItemToOwner(AircraftEntity craft, Player player) {
        if (craft.autonomous) {
            // 自主飞机（深海航母放出）没有玩家归属：不返还物品栏，也不产生任何掉落物。
            // 它的 originalStack 只是构造模板，不能流入玩家背包或地面。
            craft.setLastReturnResult("DISCARDED");
            com.piranport.debug.PiranPortDebug.aircraftReturnItem(
                    craft.getId(), craft.getOwnerUUID(), false, craft.getWeaponSlotIndex(), "DISCARDED");
            return false;
        }

        ItemStack returnStack = buildReturnStack(craft);
        int weaponSlotIndex = craft.getWeaponSlotIndex();
        // 直接尝试放回原武器槽：若空则成功，否则原版 placeItemBackInInventory 会自动入库/掉落
        if (weaponSlotIndex == 40) {
            if (player.getInventory().offhand.get(0).isEmpty()) {
                player.getInventory().offhand.set(0, returnStack);
                craft.setLastReturnResult("SUCCESS");
                com.piranport.debug.PiranPortDebug.aircraftReturnItem(
                        craft.getId(), craft.getOwnerUUID(), true, weaponSlotIndex, "SUCCESS");
                return true;
            }
        } else if (weaponSlotIndex >= 0 && weaponSlotIndex < player.getInventory().items.size()
                && player.getInventory().items.get(weaponSlotIndex).isEmpty()) {
            player.getInventory().items.set(weaponSlotIndex, returnStack);
            craft.setLastReturnResult("SUCCESS");
            com.piranport.debug.PiranPortDebug.aircraftReturnItem(
                    craft.getId(), craft.getOwnerUUID(), true, weaponSlotIndex, "SUCCESS");
            return true;
        }
        // 槽位已满：placeItemBackInInventory 返回 void，要么合并要么掉落
        // 通过调用前后槽位变化检测是否合并成功
        int beforeCount = player.getInventory().items.stream()
                .mapToInt(s -> s.isEmpty() ? 0 : s.getCount()).sum();
        player.getInventory().placeItemBackInInventory(returnStack);
        int afterCount = player.getInventory().items.stream()
                .mapToInt(s -> s.isEmpty() ? 0 : s.getCount()).sum();
        String result = afterCount > beforeCount ? "MERGED" : "SLOT_FULL";
        craft.setLastReturnResult(result);
        com.piranport.debug.PiranPortDebug.aircraftReturnItem(
                craft.getId(), craft.getOwnerUUID(), true, weaponSlotIndex, result);
        return afterCount > beforeCount;
    }

    static ItemStack buildReturnStack(AircraftEntity craft) {
        // 创建时保存的原始物品栈是唯一真源；没有它说明实体不是从物品放飞的，直接返回空。
        ItemStack originalStack = craft.getOriginalStack();
        if (originalStack == null || originalStack.isEmpty()) return ItemStack.EMPTY;
        ItemStack stack = originalStack.copy();
        // 返航后必须重新装填燃油，不能把出击时的油量带回去。
        AircraftState state = stack.get(ModDataComponents.AIRCRAFT_STATE.get());
        stack.set(ModDataComponents.AIRCRAFT_STATE.get(),
                state == null ? AircraftState.EMPTY : state.withCurrentFuel(0));
        return stack;
    }

    // ====================================================================
    // 补给（燃料 / 挂载弹药）
    // ====================================================================

    /**
     * Attempt to draw one fuel item from the owner's inventory.
     * Returns true if successful.
     */
    public static boolean tryAutoResupplyFuel(AircraftEntity craft, Player owner) {
        if (CreativeAmmoCache.isInfinite(owner, ModItems.FUEL.get())) {
            craft.setCurrentFuel(craft.getFuelCapacity());
            return true;
        }
        for (ItemStack ammo : owner.getInventory().items) {
            if (ammo.is(ModItems.FUEL.get()) && ammo.getCount() > 0) {
                ammo.shrink(1);
                craft.setCurrentFuel(craft.getFuelCapacity());
                return true;
            }
        }
        ItemStack offhand = owner.getInventory().offhand.get(0);
        if (offhand.is(ModItems.FUEL.get()) && offhand.getCount() > 0) {
            offhand.shrink(1);
            craft.setCurrentFuel(craft.getFuelCapacity());
            return true;
        }
        return false;
    }
}
