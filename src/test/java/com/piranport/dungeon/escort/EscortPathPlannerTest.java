package com.piranport.dungeon.escort;

import com.piranport.dungeon.escort.EscortPathPlanner.Cell;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EscortPathPlannerTest {

    private static boolean[][] open(int n) {
        boolean[][] grid = new boolean[n][n];
        for (boolean[] row : grid) Arrays.fill(row, true);
        return grid;
    }

    @Test
    void straightPathOnOpenWater() {
        List<Cell> path = EscortPathPlanner.plan(open(8), new Cell(0, 0), new Cell(7, 0));
        assertEquals(8, path.size());
        assertEquals(new Cell(0, 0), path.get(0));
        assertEquals(new Cell(7, 0), path.get(path.size() - 1));
        for (Cell c : path) assertEquals(0, c.z());
    }

    @Test
    void detourAroundLandWall() {
        boolean[][] grid = open(9);
        for (int z = 0; z < 8; z++) grid[4][z] = false;
        List<Cell> path = EscortPathPlanner.plan(grid, new Cell(0, 4), new Cell(8, 4));

        assertFalse(path.isEmpty());
        assertTrue(path.contains(new Cell(4, 8)));
        for (Cell c : path) assertTrue(grid[c.x()][c.z()]);
    }

    @Test
    void landGoalSnapsToNearestWaterCell() {
        boolean[][] grid = open(5);
        grid[4][2] = false;
        List<Cell> path = EscortPathPlanner.plan(grid, new Cell(0, 2), new Cell(4, 2));

        Cell end = path.get(path.size() - 1);
        assertTrue(grid[end.x()][end.z()]);
        assertEquals(1, Math.abs(end.x() - 4) + Math.abs(end.z() - 2));
    }

    @Test
    void wallWithoutGapHasNoPath() {
        boolean[][] grid = open(5);
        for (int z = 0; z < 5; z++) grid[2][z] = false;
        assertTrue(EscortPathPlanner.plan(grid, new Cell(0, 0), new Cell(4, 0)).isEmpty());
    }

    @Test
    void allLandHasNoPath() {
        assertTrue(EscortPathPlanner.plan(new boolean[4][4], new Cell(0, 0), new Cell(3, 3)).isEmpty());
    }

    @Test
    void diagonalCannotCutBetweenTwoLandCorners() {
        boolean[][] grid = open(3);
        grid[1][0] = false;
        grid[0][1] = false;
        assertTrue(EscortPathPlanner.plan(grid, new Cell(0, 0), new Cell(1, 1)).isEmpty());
    }
}
