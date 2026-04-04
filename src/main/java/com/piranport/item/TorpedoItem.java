package com.piranport.item;

import net.minecraft.world.item.Item;

public class TorpedoItem extends Item {
    private final int caliber;
    private final boolean magnetic;

    public TorpedoItem(Properties properties, int caliber) {
        this(properties, caliber, false);
    }

    public TorpedoItem(Properties properties, int caliber, boolean magnetic) {
        super(properties);
        this.caliber = caliber;
        this.magnetic = magnetic;
    }

    public int getCaliber() {
        return caliber;
    }

    public boolean isMagnetic() {
        return magnetic;
    }
}
