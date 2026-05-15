package com.piranport.artillery.config;

import java.util.List;

/** JSON 火炮定义数据模型 */
public record ArtilleryCannonData(
        int caliber,
        int barrels,
        float damage,
        int reloadTime,
        int durability,
        float scopeZoom,
        List<MuzzlePos> muzzles
) {
    public static final ArtilleryCannonData DEFAULT = new ArtilleryCannonData(
            14, 1, 6f, 30, 500, 4.0f, List.of(new MuzzlePos(0, 0, 0))
    );
}
