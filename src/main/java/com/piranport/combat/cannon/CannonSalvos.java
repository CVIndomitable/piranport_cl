package com.piranport.combat.cannon;

import com.piranport.combat.TransformationManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import java.util.List;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import com.piranport.item.ShipCoreItem;
import static com.piranport.combat.cannon.CannonInventory.findCoreSlotIndex;
import static com.piranport.combat.cannon.CannonInventory.findMatchingArtillerySlots;
import static com.piranport.combat.cannon.CannonReloading.syncAmmoToSiblingGuns;
import static com.piranport.combat.cannon.CannonAim.decodeAim;

/** 同型火炮齐射：收集可射击武器并提交延迟调度。 */
public final class CannonSalvos {
    private CannonSalvos() {}

    public static void beginSalvo(ServerPlayer player, Item weaponType,
                                   int aimMode, double ax, double ay, double az) {
        if (!player.isAlive() || player.isSpectator()
                || player.getMainHandItem().getItem() != weaponType) return;
        Inventory inv = player.getInventory();
        int weaponSlot = inv.selected;
        int coreSlot = findCoreSlotIndex(inv, player, weaponSlot);
        if (coreSlot == -1) return;

        ItemStack coreStack;
        if (coreSlot == 40) {
            coreStack = inv.offhand.get(0);
        } else if (coreSlot == -2) {
            coreStack = TransformationManager.getCoreFromConfiguredSlot(player);
        } else {
            coreStack = inv.items.get(coreSlot);
        }
        if (coreStack.isEmpty()) return;

        // 弹种同步
        syncAmmoToSiblingGuns(player);

        // 收集同型可用槽位
        List<int[]> allSlots = findMatchingArtillerySlots(player, weaponType, coreStack);
        if (allSlots.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.piranport.no_ready_gun"), true);
            return;
        }

        // 第一个立即发射
        int[] first = allSlots.remove(0);
        ItemStack firstWeapon = first[0] == 40 ? inv.offhand.get(0) : inv.items.get(first[0]);
        CannonAim aim = decodeAim(aimMode, ax, ay, az);
        CannonFiring.fireLoadedCannon(player.level(), player, coreStack, inv, first[0], first[1],
                firstWeapon, aim);

        // 剩余 → 延迟调度
        if (!allSlots.isEmpty()) {
            com.piranport.combat.SalvoManager.schedule(player, weaponType, allSlots,
                    aimMode, ax, ay, az);
        }
    }

    public static void executeSalvoFire(ServerLevel level, ServerPlayer player,
                                         int weaponSlot, int coreSlot, Item expectedWeaponType,
                                         int aimMode, double ax, double ay, double az) {
        if (!player.isAlive() || player.isSpectator()) return;
        if (player.level() != level) return;

        Inventory inv = player.getInventory();
        ItemStack weapon = CannonInventory.weaponAt(inv, weaponSlot);
        if (weapon.isEmpty() || weapon.getItem() != expectedWeaponType) return;

        ItemStack coreStack = CannonInventory.coreAt(player, coreSlot);
        if (coreStack.isEmpty() || !(coreStack.getItem() instanceof ShipCoreItem)
                || !TransformationManager.isTransformed(coreStack)) return;

        if (!CannonAmmoRules.isCannonReadyToFire(weapon, level)) return;

        CannonAim aim = decodeAim(aimMode, ax, ay, az);
        CannonFiring.fireLoadedCannon(level, player, coreStack, inv, weaponSlot, coreSlot,
                weapon, aim);
    }
}
