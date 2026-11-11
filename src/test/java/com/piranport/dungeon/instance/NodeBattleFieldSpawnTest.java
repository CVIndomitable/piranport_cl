package com.piranport.dungeon.instance;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeBattleFieldSpawnTest {
    @Test
    void normalFleetAlwaysSpawnsInFrontAndWithinLowTierDetectionRange() {
        for (BlockPos playerSpawn : new BlockPos[]{new BlockPos(1152, 64, 1280), new BlockPos(-128, 64, -256)}) {
            BlockPos center = NodeBattleField.normalFleetCenter(playerSpawn);
            for (int degrees = 0; degrees < 360; degrees++) {
                var position = NodeBattleField.fleetPosition(center, NodeBattleField.NORMAL_FLEET_RADIUS,
                        Math.toRadians(degrees));
                double dx = position.x - playerSpawn.getX();
                double dz = position.z - playerSpawn.getZ();
                assertTrue(dx > 0);
                assertTrue(Math.hypot(dx, dz) <= 30.000001);
            }
        }
    }
}
