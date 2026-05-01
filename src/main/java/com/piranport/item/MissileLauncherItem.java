package com.piranport.item;

import net.minecraft.world.item.Item;

public class MissileLauncherItem extends Item {
    private final int burstCount;
    private final Item ammoItem;
    private final boolean canReloadInFacility;

    public MissileLauncherItem(int burstCount, Item ammoItem, boolean canReloadInFacility, Properties props) {
        super(props);
        this.burstCount = burstCount;
        this.ammoItem = ammoItem;
        this.canReloadInFacility = canReloadInFacility;
    }

    public int getBurstCount() {
        return burstCount;
    }

    public Item getAmmoItem() {
        return ammoItem;
    }

    public boolean canReloadInFacility() {
        return canReloadInFacility;
    }
}
