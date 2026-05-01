package com.piranport.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ShipCoreItem extends Item {

    public enum ShipType {
        SMALL(4, 8, 2),
        MEDIUM(6, 12, 4),
        LARGE(8, 16, 6),
        SUBMARINE(3, 6, 2);

        public final int weaponSlots;
        public final int ammoSlots;
        public final int upgradeSlots;

        ShipType(int weaponSlots, int ammoSlots, int upgradeSlots) {
            this.weaponSlots = weaponSlots;
            this.ammoSlots = ammoSlots;
            this.upgradeSlots = upgradeSlots;
        }

        public int totalSlots() {
            return weaponSlots + ammoSlots + upgradeSlots;
        }
    }

    private final ShipType shipType;

    public ShipCoreItem(Properties properties, ShipType shipType) {
        super(properties);
        this.shipType = shipType;
    }

    public ShipType getShipType() {
        return shipType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Placeholder: full implementation in later phase
        return InteractionResultHolder.pass(stack);
    }

    // Placeholder methods for network packet usage
    public static boolean matchesCaliber(ItemStack ammo, ItemStack weapon) {
        // Placeholder: will be implemented in later phase
        return false;
    }

    public static boolean tryFireFromInventory(Level level, Player player, InteractionHand hand) {
        // Placeholder: will be implemented in later phase
        return false;
    }
}
