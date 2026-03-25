package com.piranport.item;

import net.minecraft.world.item.Item;

public class TorpedoLauncherItem extends Item {
    private final int caliber;
    private final int tubeCount;
    private final int cooldownTicks;

    public TorpedoLauncherItem(Properties properties, int caliber, int tubeCount, int cooldownTicks) {
        super(properties);
        this.caliber = caliber;
        this.tubeCount = tubeCount;
        this.cooldownTicks = cooldownTicks;
    }

    public int getCaliber() {
        return caliber;
    }

    public int getTubeCount() {
        return tubeCount;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }
}
