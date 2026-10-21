package kz.aitu.daa.algorithms;

import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;

public final class Kahn {
    private Kahn() { }
    public static int[] order(Graph graph, Metrics metrics) {
        graph.requireDirected(true);
        int n = graph.n(), read = 0, write = 0;
        int[] degree = new int[n], queue = new int[n];
        for (int v = 0; v < n; v++) {
            degree[v] = graph.inDegree(v);
            if (degree[v] == 0) queue[write++] = v;
        }
        while (read < write) {
            int v = queue[read++];
            for (int a = graph.firstArc(v); a != -1; a = graph.nextArc(a)) {
                metrics.edgeScans++;
                int u = graph.arcTo(a);
                if (--degree[u] == 0) queue[write++] = u;
            }
        }
        if (write != n) throw new IllegalArgumentException("Cycle detected: processed " + write + "/" + n + " vertices");
        return queue;
    }
    /** Strictly validates a permutation as well as every edge constraint. */
    public static boolean isValidOrder(Graph graph, int[] order) {
        graph.requireDirected(true);
        if (order.length != graph.n()) return false;
        int[] position = new int[graph.n()];
        boolean[] seen = new boolean[graph.n()];
        for (int i = 0; i < order.length; i++) {
            int v = order[i];
            if (v < 0 || v >= graph.n() || seen[v]) return false;
            seen[v] = true; position[v] = i;
        }
        for (int e = 0; e < graph.m(); e++)
            if (position[graph.from(e)] >= position[graph.to(e)]) return false;
        return true;
    }
}
