package com.piranport.item;

import net.minecraft.world.item.Item;

public class SonarItem extends Item {
    private final int weight;

    public SonarItem(Properties properties, int weight) {
        super(properties);
        this.weight = weight;
    }

    public int getWeight() { return weight; }
}
