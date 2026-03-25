package com.piranport.item;

import net.minecraft.world.item.Item;

public class TorpedoItem extends Item {
    private final int caliber;

    public TorpedoItem(Properties properties, int caliber) {
        super(properties);
        this.caliber = caliber;
    }

    public int getCaliber() {
        return caliber;
    }
}
