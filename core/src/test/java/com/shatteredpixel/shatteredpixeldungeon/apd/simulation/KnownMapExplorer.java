package com.shatteredpixel.shatteredpixeldungeon.apd.simulation;

import java.util.Arrays;

// Test-only navigation. Hidden terrain is never used to select a destination.
public final class KnownMapExplorer {
    private KnownMapExplorer() {}
    public static int step(int width, int start, boolean[] known, boolean[] walkable,
                           int[] visits, int forbidden) {
        if (width < 1 || known.length % width != 0 || walkable.length != known.length
                || visits.length != known.length || start < 0 || start >= known.length)
            throw new IllegalArgumentException("invalid map");
        int[] distance = new int[known.length], first = new int[known.length], queue = new int[known.length];
        Arrays.fill(distance, -1);
        distance[start] = 0;
        queue[0] = start;
        int head = 0, tail = 1, best = -1;
        long bestScore = Long.MAX_VALUE;
        while (head < tail) {
            int cell = queue[head++];
            if (cell != start && cell != forbidden) {
                long score = (long) visits[cell] * (known.length + 1L) + distance[cell];
                if (score < bestScore) { bestScore = score; best = cell; }
            }
            int x = cell % width, y = cell / width;
            for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx, ny = y + dy;
                if (nx < 0 || nx >= width || ny < 0 || ny >= known.length / width) continue;
                int next = ny * width + nx;
                if (next == forbidden || distance[next] >= 0 || !known[next] || !walkable[next]) continue;
                distance[next] = distance[cell] + 1;
                first[next] = cell == start ? next : first[cell];
                queue[tail++] = next;
            }
        }
        return best < 0 ? -1 : first[best];
    }
}
