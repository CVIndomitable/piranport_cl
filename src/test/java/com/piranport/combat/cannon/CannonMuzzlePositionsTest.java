package com.piranport.combat.cannon;

import com.piranport.artillery.config.MuzzlePos;
import com.piranport.rigging.RiggingGeometry;
import com.piranport.rigging.RiggingLoadoutResolver;
import com.piranport.rigging.RiggingSocket;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CannonMuzzlePositionsTest {
    private static final Vec3 TIP = new Vec3(0, -4.5, 16);

    @Test
    void mainHandMuzzleStaysLeftFrontWithOrWithoutDeck() {
        var normal = RiggingGeometry.sideMount(RiggingLoadoutResolver.sideSockets(false).getFirst());
        var carrier = RiggingGeometry.sideMount(RiggingLoadoutResolver.sideSockets(true).getFirst());
        Vec3 normalTip = CannonMuzzlePositions.worldMuzzle(Vec3.ZERO, 0, 1, false, normal, TIP);
        Vec3 carrierTip = CannonMuzzlePositions.worldMuzzle(Vec3.ZERO, 0, 1, false, carrier, TIP);

        assertTrue(carrierTip.x > 0 && carrierTip.z > 0);
        assertEquals(normalTip, carrierTip);
        Vec3 turned = CannonMuzzlePositions.worldMuzzle(Vec3.ZERO, 90, 1, false, carrier, TIP);
        assertEquals(-carrierTip.z, turned.x, 1e-5);
        assertEquals(carrierTip.x, turned.z, 1e-5);
    }

    @Test
    void separateBarrelsKeepTheirOwnTips() {
        var mount = RiggingGeometry.sideMount(RiggingSocket.SIDE_LEFT_2);
        Vec3 a = CannonMuzzlePositions.worldMuzzle(Vec3.ZERO, 0, 1, false, mount,
                CannonMuzzlePositions.modelTip("chinese_twin_140mm_gun", 0, new MuzzlePos(0.3, 0.2, 0)));
        Vec3 b = CannonMuzzlePositions.worldMuzzle(Vec3.ZERO, 0, 1, false, mount,
                CannonMuzzlePositions.modelTip("chinese_twin_140mm_gun", 1, new MuzzlePos(-0.3, 0.2, 0)));
        assertTrue(a.x > b.x && b.x > 0);
        assertEquals(4.5 * RiggingGeometry.SIDE_ITEM_SCALE / 16 * 0.9375, a.x - b.x, 1e-6);
        assertEquals(a.z, b.z, 1e-6);
        assertEquals(a.y, b.y, 1e-6);
    }

    @Test
    void type79UsesTheNewMountedBarrelTips() {
        var legacy = new MuzzlePos(0.15, 0.2, 0);
        assertEquals(new Vec3(2, 1, 17), CannonMuzzlePositions.modelTip("type79_twin_100mm_gun", 0, legacy));
        assertEquals(new Vec3(-2, 1, 17), CannonMuzzlePositions.modelTip("type79_twin_100mm_gun", 1, legacy));
    }

    @Test
    void usSingleFiveInchUsesModeledMuzzleTip() {
        assertModeledTip("us_single_5inch_gun", 0, 0, 6.8, 17.85);
    }

    @Test
    void compactTwinGunsUseBothScaledMuzzleTips() {
        assertModeledTip("japanese_127mm_twin_gun", 0, 1.8, 4.8, 14.4);
        assertModeledTip("japanese_127mm_twin_gun", 1, -1.8, 4.8, 14.4);
        assertModeledTip("us_twin_5inch_gun", 0, 2.1, 5.1, 14.4);
        assertModeledTip("us_twin_5inch_gun", 1, -2.1, 5.1, 14.4);
        assertModeledTip("us_twin_5inch_dp_gun", 0, 2.4, 5.4, 14.4);
        assertModeledTip("us_twin_5inch_dp_gun", 1, -2.4, 5.4, 14.4);
    }

    private static void assertModeledTip(String model, int barrel, double x, double y, double z) {
        Vec3 tip = CannonMuzzlePositions.modelTip(model, barrel, new MuzzlePos(0, 0, 0));
        assertEquals(x, tip.x, 1e-6);
        assertEquals(y, tip.y, 1e-6);
        assertEquals(z, tip.z, 1e-6);
    }

    @Test
    void twinFiveInchL54UsesBothFlushMuzzleTips() {
        assertModeledTip("twin_5inch_l54_dp_gun", 0, 1, 2.25, 15);
        assertModeledTip("twin_5inch_l54_dp_gun", 1, -1, 2.25, 15);
    }

    @Test
    void japaneseSingle140mmUsesTheModeledMuzzleTip() {
        assertModeledTip("japanese_single_140mm_gun", 0, 0, 2.8, 16.8);
    }

    @Test
    void britishSingleSixInchUsesTheModeledMuzzleTip() {
        assertModeledTip("british_single_6inch_gun", 0, 0, 5.75, 16.75);
    }

    @Test
    void britishTripleSixInchUsesAllThreeMountedMuzzleTips() {
        assertModeledTip("british_triple_6inch_gun", 0, 3.34, 3.90, 23.25);
        assertModeledTip("british_triple_6inch_gun", 1, 0, 3.90, 23.25);
        assertModeledTip("british_triple_6inch_gun", 2, -3.34, 3.90, 23.25);
    }

    @Test
    void japaneseSingle152mmUsesTheModeledMuzzleTip() {
        assertModeledTip("japanese_single_152mm_gun", 0, 0, 5.25, 18);
    }

    @Test
    void britishQuadFourteenInchUsesFourMountedMuzzleTips() {
        assertModeledTip("british_quad_14inch_gun", 0, 4.65, 5.15, 23.25);
        assertModeledTip("british_quad_14inch_gun", 1, 1.55, 5.15, 23.25);
        assertModeledTip("british_quad_14inch_gun", 2, -1.55, 5.15, 23.25);
        assertModeledTip("british_quad_14inch_gun", 3, -4.65, 5.15, 23.25);
        assertModeledTip("british_quad_14inch_gun_284", 0, 4.65, 3.65, 23.25);
        assertModeledTip("british_quad_14inch_gun_284", 1, 1.55, 3.65, 23.25);
        assertModeledTip("british_quad_14inch_gun_284", 2, -1.55, 3.65, 23.25);
        assertModeledTip("british_quad_14inch_gun_284", 3, -4.65, 3.65, 23.25);
    }

    @Test
    void britishTwinFifteenInchUsesBothMountedMuzzleTips() {
        assertModeledTip("british_twin_15inch_gun", 0, 2.92, 3.8, 23.25);
        assertModeledTip("british_twin_15inch_gun", 1, -2.92, 3.8, 23.25);
        assertModeledTip("british_twin_15inch_gun_kai", 0, 2.92, 3.8, 23.25);
        assertModeledTip("british_twin_15inch_gun_kai", 1, -2.92, 3.8, 23.25);
    }

    @Test
    void germanTwin380mmUsesBothMountedMuzzleTips() {
        assertModeledTip("german_twin_380mm_gun", 0, 2.92, 3.9, 23.25);
        assertModeledTip("german_twin_380mm_gun", 1, -2.92, 3.9, 23.25);
    }

    @Test
    void germanTwin406mmUsesBothMountedMuzzleTips() {
        assertModeledTip("german_twin_406mm_gun", 0, 2.55, 2.55, 23.25);
        assertModeledTip("german_twin_406mm_gun", 1, -2.55, 2.55, 23.25);
    }

    @Test
    void germanSkc40UsesAllThreeMountedMuzzleTips() {
        assertModeledTip("german_skc40_triple_420mm_gun", 0, 3.42, 3.10, 23.25);
        assertModeledTip("german_skc40_triple_420mm_gun", 1, 0, 3.10, 23.25);
        assertModeledTip("german_skc40_triple_420mm_gun", 2, -3.42, 3.10, 23.25);
    }

    @Test
    void britishSingleEighteenInchUsesTheMountedMuzzleTip() {
        assertModeledTip("british_single_18inch_gun", 0, 0, 3.74, 23.25);
    }

    @Test
    void britishTwinEighteenInchUsesBothMountedMuzzleTips() {
        assertModeledTip("british_twin_18inch_gun", 0, 2.45, 3.72, 23.25);
        assertModeledTip("british_twin_18inch_gun", 1, -2.45, 3.72, 23.25);
    }

    @Test
    void usTwinEighteenInchUsesBothMountedMuzzleTips() {
        assertModeledTip("us_twin_18inch_gun", 0, 2.70, 4.20, 23.25);
        assertModeledTip("us_twin_18inch_gun", 1, -2.70, 4.20, 23.25);
    }

    @Test
    void japaneseTripleFortySixCentimeterUsesAllThreeMountedMuzzleTips() {
        assertModeledTip("japanese_triple_460mm_gun", 0, 3.85, 4.38, 23.25);
        assertModeledTip("japanese_triple_460mm_gun", 1, 0, 4.38, 23.25);
        assertModeledTip("japanese_triple_460mm_gun", 2, -3.85, 4.38, 23.25);
    }

    @Test
    void sovietTriple460mmUsesAllThreeMountedMuzzleTips() {
        assertModeledTip("soviet_triple_460mm_gun", 0, 3.72, 3.58, 23.25);
        assertModeledTip("soviet_triple_460mm_gun", 1, 0, 3.58, 23.25);
        assertModeledTip("soviet_triple_460mm_gun", 2, -3.72, 3.58, 23.25);
    }

    @Test
    void britishTwinTwentyInchUsesBothMountedMuzzleTips() {
        assertModeledTip("british_twin_20inch_gun", 0, 2.45, 3.62, 23.25);
        assertModeledTip("british_twin_20inch_gun", 1, -2.45, 3.62, 23.25);
    }

    @Test
    void usTwinTwentyInchUsesBothMountedMuzzleTips() {
        assertModeledTip("us_twin_20inch_gun", 0, 2.88, 3.42, 23.25);
        assertModeledTip("us_twin_20inch_gun", 1, -2.88, 3.42, 23.25);
    }

    @Test
    void frenchQuad380UsesAllFourMountedMuzzleTips() {
        assertModeledTip("french_quad_380mm_gun", 0, 4.50, 3.72, 23.25);
        assertModeledTip("french_quad_380mm_gun", 1, 1.95, 3.72, 23.25);
        assertModeledTip("french_quad_380mm_gun", 2, -1.95, 3.72, 23.25);
        assertModeledTip("french_quad_380mm_gun", 3, -4.50, 3.72, 23.25);
    }

    @Test
    void fourteenBarrelGunUsesEveryMountedMuzzleTip() {
        String model = "fourteen_barrel_gun";
        assertModeledTip(model, 0, 7.49, 3.65, 19.60);
        assertModeledTip(model, 1, 6.12, 3.65, 19.60);
        assertModeledTip(model, 2, 5.06, 4.95, 7.55);
        assertModeledTip(model, 3, 4.33, 7.82, 0.45);
        assertModeledTip(model, 4, 3.79, 4.95, 7.55);
        assertModeledTip(model, 5, 3.19, 7.82, 0.45);
        assertModeledTip(model, 6, 1.63, 6.50, 23.25);
        assertModeledTip(model, 7, -1.63, 6.50, 23.25);
        assertModeledTip(model, 8, -3.19, 7.82, 0.45);
        assertModeledTip(model, 9, -3.79, 4.95, 7.55);
        assertModeledTip(model, 10, -4.33, 7.82, 0.45);
        assertModeledTip(model, 11, -5.06, 4.95, 7.55);
        assertModeledTip(model, 12, -6.12, 3.65, 19.60);
        assertModeledTip(model, 13, -7.49, 3.65, 19.60);
    }

    @Test
    void fourteenBarrelGunFallsBackForExtraDataPackMuzzles() {
        Vec3 tip = CannonMuzzlePositions.modelTip("fourteen_barrel_gun", 14, new MuzzlePos(-1, 0, 0));
        assertEquals(new Vec3(-6, -4.5, 16), tip);
    }

    @Test
    void britishTripleSixteenInchUsesAllThreeMountedMuzzleTips() {
        assertModeledTip("british_triple_16inch_gun", 0, 3.3, 4.4, 23.25);
        assertModeledTip("british_triple_16inch_gun", 1, 0, 4.4, 23.25);
        assertModeledTip("british_triple_16inch_gun", 2, -3.3, 4.4, 23.25);
        assertModeledTip("british_triple_16inch_gun_kai", 0, 3.3, 4.4, 23.25);
        assertModeledTip("british_triple_16inch_gun_kai", 1, 0, 4.4, 23.25);
        assertModeledTip("british_triple_16inch_gun_kai", 2, -3.3, 4.4, 23.25);
    }

    @Test
    void usTripleSixteenInchMk6UsesAllThreeMountedMuzzleTips() {
        assertModeledTip("us_triple_16inch_gun_mk6", 0, 3.28, 3.8, 23.25);
        assertModeledTip("us_triple_16inch_gun_mk6", 1, 0, 3.8, 23.25);
        assertModeledTip("us_triple_16inch_gun_mk6", 2, -3.28, 3.8, 23.25);
        assertModeledTip("us_triple_16inch_gun_mk7", 0, 3.44, 4.14, 23.25);
        assertModeledTip("us_triple_16inch_gun_mk7", 1, 0, 4.14, 23.25);
        assertModeledTip("us_triple_16inch_gun_mk7", 2, -3.44, 4.14, 23.25);
    }

    @Test
    void usTwinSixteenInchKaiUsesBothMountedMuzzleTips() {
        assertModeledTip("us_twin_16inch_gun_kai", 0, 2.85, 3.15, 23.25);
        assertModeledTip("us_twin_16inch_gun_kai", 1, -2.85, 3.15, 23.25);
    }

    @Test
    void obstructedMuzzleStaysOnShootersSideOfWall() {
        Vec3 eye = new Vec3(0, 2, 0);
        assertEquals(new Vec3(0, 2, 0.45), CannonMuzzlePositions.beforeObstruction(eye, eye.add(0, 0, 0.50)));
        assertEquals(eye, CannonMuzzlePositions.beforeObstruction(eye, eye.add(0, 0, 0.01)));
    }
}
