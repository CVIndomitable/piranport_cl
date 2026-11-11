package com.piranport.dungeon.instance;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeBattleFieldSpawnTest {
    @Test
    void normalFleetStaysNearEnemyCorner() {
        for (BlockPos center : new BlockPos[]{new BlockPos(1408, 64, 1408), new BlockPos(-128, 64, -256)}) {
            for (int degrees = 0; degrees < 360; degrees++) {
                var position = NodeBattleField.fleetPosition(center, NodeBattleField.NORMAL_FLEET_RADIUS,
                        Math.toRadians(degrees));
                double dx = position.x - center.getX();
                double dz = position.z - center.getZ();
                assertTrue(Math.abs(Math.hypot(dx, dz) - NodeBattleField.NORMAL_FLEET_RADIUS) < 0.000001);
            }
        }
    }
}
