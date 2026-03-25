package com.piranport.item;

import net.minecraft.world.item.Item;

public class ArmorPlateItem extends Item {
    private final int armorBonus;
    private final int weight;

    public ArmorPlateItem(Properties properties, int armorBonus, int weight) {
        super(properties);
        this.armorBonus = armorBonus;
        this.weight = weight;
    }

    public int getArmorBonus() { return armorBonus; }
    public int getWeight() { return weight; }
}
