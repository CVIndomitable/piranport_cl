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
    void obstructedMuzzleStaysOnShootersSideOfWall() {
        Vec3 eye = new Vec3(0, 2, 0);
        assertEquals(new Vec3(0, 2, 0.45), CannonMuzzlePositions.beforeObstruction(eye, eye.add(0, 0, 0.50)));
        assertEquals(eye, CannonMuzzlePositions.beforeObstruction(eye, eye.add(0, 0, 0.01)));
    }
}
