package com.piranport.ammo;

public enum AmmoCategory {
    SHELL("炮弹"),
    TORPEDO("鱼雷"),
    AERIAL("航弹"),
    DEPTH_CHARGE("深弹"),
    MISSILE("导弹");

    private final String displayName;

    AmmoCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
