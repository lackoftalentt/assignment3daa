package kz.aitu.daa.algorithms;

import java.util.Arrays;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;

public final class DagShortestPath {
    private DagShortestPath() { }
    public static final long INF = Long.MAX_VALUE / 4;
    public record Result(long[] distance, int[] parent) {
        public long sum() {
            long total = 0;
            for (long d : distance) total = Math.addExact(total, d);
            return total;
        }
        public int[] pathTo(int target) {
            if (target < 0 || target >= parent.length) throw new IllegalArgumentException("Invalid target");
            int[] reversed = new int[parent.length];
            int length = 0;
            for (int v = target; v != -1; v = parent[v]) {
                if (length == parent.length) throw new IllegalStateException("Cycle in parent chain");
                reversed[length++] = v;
            }
            int[] path = new int[length];
            for (int i = 0; i < length; i++) path[i] = reversed[length - 1 - i];
            return path;
        }
    }
    public static Result run(Graph graph, Metrics metrics) {
        // The public API validates acyclicity. Keep Kahn's work separate from relaxation counters.
        int[] order = Kahn.order(graph, new Metrics());
        return relax(graph, order, metrics);
    }
    private static Result relax(Graph graph, int[] order, Metrics metrics) {
        long[] distance = new long[graph.n()];
        int[] parent = new int[graph.n()];
        Arrays.fill(distance, INF); Arrays.fill(parent, -1);
        for (int v = 0; v < graph.n(); v++) if (graph.inDegree(v) == 0) distance[v] = 0;
        for (int v : order) {
            for (int a = graph.firstArc(v); a != -1; a = graph.nextArc(a)) {
                metrics.edgeScans++;
                int u = graph.arcTo(a);
                long candidate = distance[v] + graph.arcWeight(a);
                if (distance[v] != INF && candidate < distance[u]) {
                    distance[u] = candidate; parent[u] = v;
                }
            }
        }
        return new Result(distance, parent);
    }
}
