package com.piranport.combat.cannon;

import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.artillery.config.MuzzlePos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CannonClassificationTest {
    @Test
    void caliberFamiliesUseRealCaliberBoundaries() {
        assertEquals(CannonAmmoRules.CaliberFamily.SMALL, CannonAmmoRules.familyForCaliber(4));
        assertEquals(CannonAmmoRules.CaliberFamily.MEDIUM, CannonAmmoRules.familyForCaliber(8));
        assertEquals(CannonAmmoRules.CaliberFamily.LARGE, CannonAmmoRules.familyForCaliber(16));
    }

    @Test
    void classificationDoesNotDependOnWeaponIdentity() {
        // 14 联装实验炮与普通大炮共享 LARGE 口径族；分类器不应再维护物品 ID 白名单。
        ArtilleryCannonData experimental = new ArtilleryCannonData(16, 14, 70f, 80, 3500, 4f,
                List.of(new MuzzlePos(0, 0, 0), new MuzzlePos(0, 0, 0), new MuzzlePos(0, 0, 0),
                        new MuzzlePos(0, 0, 0), new MuzzlePos(0, 0, 0), new MuzzlePos(0, 0, 0),
                        new MuzzlePos(0, 0, 0), new MuzzlePos(0, 0, 0), new MuzzlePos(0, 0, 0),
                        new MuzzlePos(0, 0, 0), new MuzzlePos(0, 0, 0), new MuzzlePos(0, 0, 0),
                        new MuzzlePos(0, 0, 0), new MuzzlePos(0, 0, 0)),
                12f, 0.008f, 9.8f, 7f, 1.8f, 10, 14, 2f,
                800f, 1.8f, 1.8f, 45f, -5f, 3f, "MANUAL");
        assertEquals(CannonAmmoRules.CaliberFamily.LARGE, CannonAmmoRules.familyForData(experimental));
    }
}
