package com.piranport.dungeon.escort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * 护航运输船的格网 A*：8 邻接，斜穿时两侧直邻格都要可通行，避免擦着陆地角穿过去。
 *
 * <p>纯计算，不依赖世界；可通行与否由调用方按 4 格单元采样后传入。</p>
 */
public final class EscortPathPlanner {

    private static final double SQRT2 = Math.sqrt(2.0);

    public record Cell(int x, int z) {}

    private record Entry(int index, double f) {}

    private EscortPathPlanner() {}

    /**
     * 返回从起点到目标的单元序列（含两端）。目标不可通行时就近换成最近的可通行单元；
     * 整张格网都不可通行、或找不到连通路径时返回空列表。起点即使不可通行也允许出发，
     * 因为运输船可能恰好停在岸边或平台上。
     */
    public static List<Cell> plan(boolean[][] passable, Cell start, Cell goal) {
        Cell end = nearestPassable(passable, goal);
        if (end == null) return Collections.emptyList();

        int n = passable.length;
        int startIndex = index(start, n);
        int endIndex = index(end, n);
        double[] g = new double[n * n];
        Arrays.fill(g, Double.POSITIVE_INFINITY);
        int[] came = new int[n * n];
        Arrays.fill(came, -1);
        boolean[] closed = new boolean[n * n];

        PriorityQueue<Entry> open = new PriorityQueue<>((a, b) -> Double.compare(a.f(), b.f()));
        g[startIndex] = 0.0;
        open.add(new Entry(startIndex, heuristic(start, end)));

        while (!open.isEmpty()) {
            int current = open.poll().index();
            if (closed[current]) continue;
            if (current == endIndex) return reconstruct(came, startIndex, endIndex, n);
            closed[current] = true;

            int cx = current / n;
            int cz = current % n;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    int nx = cx + dx;
                    int nz = cz + dz;
                    if (nx < 0 || nz < 0 || nx >= n || nz >= n || !passable[nx][nz]) continue;
                    boolean diagonal = dx != 0 && dz != 0;
                    if (diagonal && (!passable[cx + dx][cz] || !passable[cx][cz + dz])) continue;

                    int next = nx * n + nz;
                    if (closed[next]) continue;
                    double tentative = g[current] + (diagonal ? SQRT2 : 1.0);
                    if (tentative < g[next]) {
                        g[next] = tentative;
                        came[next] = current;
                        Cell nextCell = new Cell(nx, nz);
                        open.add(new Entry(next, tentative + heuristic(nextCell, end)));
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private static Cell nearestPassable(boolean[][] passable, Cell goal) {
        if (passable[goal.x()][goal.z()]) return goal;
        Cell best = null;
        long bestDistance = Long.MAX_VALUE;
        for (int x = 0; x < passable.length; x++) {
            for (int z = 0; z < passable[x].length; z++) {
                if (!passable[x][z]) continue;
                long dx = x - goal.x();
                long dz = z - goal.z();
                long distance = dx * dx + dz * dz;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new Cell(x, z);
                }
            }
        }
        return best;
    }

    private static List<Cell> reconstruct(int[] came, int startIndex, int endIndex, int n) {
        List<Cell> path = new ArrayList<>();
        for (int at = endIndex; at != -1; at = came[at]) {
            path.add(new Cell(at / n, at % n));
            if (at == startIndex) break;
        }
        Collections.reverse(path);
        return path;
    }

    private static int index(Cell cell, int n) {
        return cell.x() * n + cell.z();
    }

    private static double heuristic(Cell a, Cell b) {
        int dx = Math.abs(a.x() - b.x());
        int dz = Math.abs(a.z() - b.z());
        return Math.max(dx, dz) + (SQRT2 - 1.0) * Math.min(dx, dz);
    }
}
