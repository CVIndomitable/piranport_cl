package com.piranport.item;

/** 舰装核心类型枚举 */
public enum ShipType {
    //                  hp   cost  wpn ammo enh fuel dist  满载  空载  护甲 韧性 nationality
    SMALL(  0,  40, 4, 4, 2, 10, 100.0, 1.15, 1.4,   8,  4, "U"),
    MEDIUM(10,  64, 5, 4, 3, 20,  70.0, 1.0,  1.2,  12,  8, "U"),
    LARGE( 20, 112, 6, 4, 4, 30,  50.0, 0.85, 1.0,  16, 12, "U"),
    SUBMARINE(-8, 32, 4, 4, 2, 10, 100.0, 0.7, 0.8,  4,  0, "U");

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
    /**
     * 国籍字段：策划决策/航空/22-放飞动作按国籍特化.md
     * <p>J = 日系、E = 英系、U = 美系、其他走通用美系 F4U 兜底动作。</p>
     */
    public final String nationality;

    ShipType(int healthBonus, int maxLoad, int weaponSlots, int ammoSlots, int enhancementSlots,
             int fuelCapacity, double distancePerFuel,
             double fullLoadSpeed, double emptySpeed, int baseArmor, int armorToughness,
             String nationality) {
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
        this.nationality = nationality;
    }

    public int totalSlots() {
        return weaponSlots + ammoSlots + enhancementSlots;
    }

    /** 国籍放飞动作：依据 航空/22-放飞动作按国籍特化.md */
    public String getLaunchAnimation() {
        return switch (nationality) {
            case "J" -> "BOW";          // 日系：和弓射箭
            case "E" -> "LONGBOW";      // 英系：长弓
            case "U" -> "MUSKET";       // 美系：火枪 (F4U)
            default -> "MUSKET";        // 通用兜底：美系 F4U 姿势
        };
    }
}
