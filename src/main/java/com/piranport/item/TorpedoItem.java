package com.piranport.item;

import net.minecraft.world.item.Item;

public class TorpedoItem extends Item {
    private final int caliber;

    public TorpedoItem(int caliber, Properties props) {
        super(props);
        this.caliber = caliber;
    }

    public int getCaliber() {
        return caliber;
    }
}
