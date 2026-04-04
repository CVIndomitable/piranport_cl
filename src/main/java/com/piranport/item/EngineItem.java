package com.piranport.item;

import net.minecraft.world.item.Item;

public class EngineItem extends Item {
    private final double speedBonus;
    private final int weight;

    public EngineItem(Properties properties, double speedBonus, int weight) {
        super(properties);
        this.speedBonus = speedBonus;
        this.weight = weight;
    }

    public double getSpeedBonus() { return speedBonus; }
    public int getWeight() { return weight; }
}
