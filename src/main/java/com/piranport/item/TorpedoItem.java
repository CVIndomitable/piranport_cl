package com.piranport.item;

import net.minecraft.world.item.Item;

public class TorpedoItem extends Item {
    private final int caliber;
    private final boolean magnetic;
    private final boolean wireGuided;
    private final boolean acoustic;

    public TorpedoItem(Properties properties, int caliber) {
        this(properties, caliber, false, false, false);
    }

    public TorpedoItem(Properties properties, int caliber, boolean magnetic) {
        this(properties, caliber, magnetic, false, false);
    }

    public TorpedoItem(Properties properties, int caliber, boolean magnetic, boolean wireGuided) {
        this(properties, caliber, magnetic, wireGuided, false);
    }

    public TorpedoItem(Properties properties, int caliber, boolean magnetic, boolean wireGuided, boolean acoustic) {
        super(properties);
        this.caliber = caliber;
        this.magnetic = magnetic;
        this.wireGuided = wireGuided;
        this.acoustic = acoustic;
    }

    public int getCaliber() {
        return caliber;
    }

    public boolean isMagnetic() {
        return magnetic;
    }

    public boolean isWireGuided() {
        return wireGuided;
    }

    public boolean isAcoustic() {
        return acoustic;
    }
}
