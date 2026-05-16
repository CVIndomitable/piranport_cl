package com.piranport.item;

/** 舰装核心类型枚举 */
public enum ShipType {
    //                  hp   cost  wpn ammo enh fuel dist  满载  空载  护甲 韧性
    SMALL(  0,  40, 4, 4, 2, 10, 100.0, 1.15, 1.4,   8,  4),
    MEDIUM(10,  64, 5, 4, 3, 20,  70.0, 1.0,  1.2,  12,  8),
    LARGE( 20, 112, 6, 4, 4, 30,  50.0, 0.85, 1.0,  16, 12),
    SUBMARINE(-8, 32, 4, 4, 2, 10, 100.0, 0.7, 0.8,  4,  0);

    public final int healthBonus;
    public final int maxLoad;
    public final int weaponSlots;
    public final int ammoSlots;
    public final int enhancementSlots;
    /** Fuel tank capacity in b (bucket) units. */
    public final int fuelCapacity;
    /** Distance in blocks per 1b fuel consumed. */
    public final double distancePerFuel;
    /** Speed multiplier at full load (totalLoad == maxLoad). */
    public final double fullLoadSpeed;
    /** Speed multiplier at zero load. */
    public final double emptySpeed;
    /** Base armor provided by the core itself. */
    public final int baseArmor;
    /** Armor toughness provided by the core. */
    public final int armorToughness;

    ShipType(int healthBonus, int maxLoad, int weaponSlots, int ammoSlots, int enhancementSlots,
             int fuelCapacity, double distancePerFuel,
             double fullLoadSpeed, double emptySpeed, int baseArmor, int armorToughness) {
        this.healthBonus = healthBonus;
        this.maxLoad = maxLoad;
        this.weaponSlots = weaponSlots;
        this.ammoSlots = ammoSlots;
        this.enhancementSlots = enhancementSlots;
        this.fuelCapacity = fuelCapacity;
        this.distancePerFuel = distancePerFuel;
        this.fullLoadSpeed = fullLoadSpeed;
        this.emptySpeed = emptySpeed;
        this.baseArmor = baseArmor;
        this.armorToughness = armorToughness;
    }

    public int totalSlots() {
        return weaponSlots + ammoSlots + enhancementSlots;
    }
}
