package com.piranport.item;

import net.minecraft.world.item.Item;

public class TorpedoLauncherItem extends Item {
    private final int tubeCount;
    private final int caliber;

    public TorpedoLauncherItem(int tubeCount, int caliber, Properties props) {
        super(props);
        this.tubeCount = tubeCount;
        this.caliber = caliber;
    }

    public int getTubeCount() {
        return tubeCount;
    }

    public int getCaliber() {
        return caliber;
    }
}
