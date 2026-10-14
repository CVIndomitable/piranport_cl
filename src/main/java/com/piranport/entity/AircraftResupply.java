package com.piranport.entity;

import com.piranport.component.AircraftInfo;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

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
        com.piranport.debug.PiranPortDebug.event(
                "Aircraft RETURN | type={} entityId={} forced={}",
                craft.aircraftType.name(), craft.getId(), craft.isForcedReturn());
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
                returned = returnItemToOwner(craft, owner);
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

    static boolean returnItemToOwner(AircraftEntity craft, Player player) {
        if (craft.autonomous) {
            // 自主飞机不返回物品栏，直接掉落物品
            Block.popResource(player.level(), player.blockPosition(), buildReturnStack(craft));
            craft.setLastReturnResult("DROPPED");
            com.piranport.debug.PiranPortDebug.aircraftReturnItem(
                    craft.getId(), craft.getOwnerUUID(), true, craft.getWeaponSlotIndex(), "DROPPED");
            return true;
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
        // Restore original ItemStack if available, preserving custom names/enchants/components
        ItemStack stack;
        ItemStack originalStack = craft.getOriginalStack();
        if (originalStack != null && !originalStack.isEmpty()) {
            stack = originalStack.copy();
        } else {
            stack = switch (craft.aircraftType) {
                case FIGHTER        -> new ItemStack(ModItems.SEAFIRE.get());
                case DIVE_BOMBER    -> new ItemStack(ModItems.PETREL_BOMBER.get());
                case TORPEDO_BOMBER -> new ItemStack(ModItems.SWORDFISH_TORPEDO.get());
                case LEVEL_BOMBER   -> new ItemStack(ModItems.B25_BOMBER.get());
                case ASW            -> new ItemStack(ModItems.SWORDFISH_ASW.get());
                case RECON          -> new ItemStack(ModItems.SAIUN_RECON.get());
                case ROCKET_FIGHTER -> new ItemStack(ModItems.F6F_HELLCAT_ROCKET.get());
            };
        }
        // Intentionally reset fuel to 0: returning aircraft must be refueled before next sortie
        AircraftInfo info = stack.get(ModDataComponents.AIRCRAFT_INFO.get());
        if (info != null) stack.set(ModDataComponents.AIRCRAFT_INFO.get(), info.withCurrentFuel(0));
        return stack;
    }

    // ====================================================================
    // 补给（燃料 / 挂载弹药）
    // ====================================================================

    /**
     * Attempt to draw payload items from the owner's inventory. Returns true if successful.
     * 自动装填固定关闭；保留方法作为旧实体调用方的兼容边界。
     */
    public static boolean tryAutoResupplyAmmo(AircraftEntity craft, Player owner) {
        if (craft.payloadType.isEmpty()) return false;
        // 自动补给是"空中重新装填挂载"，仅在 AUTO_RESUPPLY 开启时可用；手动模式下挂载只走 R 键背包装填
        return false;
    }

    /**
     * Attempt to draw one aviation_fuel from the owner's inventory.
     * Returns true if successful.
     */
    public static boolean tryAutoResupplyFuel(AircraftEntity craft, Player owner) {
        for (ItemStack ammo : owner.getInventory().items) {
            if (ammo.is(ModItems.AVIATION_FUEL.get()) && ammo.getCount() > 0) {
                ammo.shrink(1);
                craft.setCurrentFuel(craft.getFuelCapacity());
                return true;
            }
        }
        ItemStack offhand = owner.getInventory().offhand.get(0);
        if (offhand.is(ModItems.AVIATION_FUEL.get()) && offhand.getCount() > 0) {
            offhand.shrink(1);
            craft.setCurrentFuel(craft.getFuelCapacity());
            return true;
        }
        return false;
    }
}
