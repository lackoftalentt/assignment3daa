package kz.aitu.daa.algorithms;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;

/** Bonus B: parallel inter-component dependencies keep their maximum weight.
 * SCC-internal edges are discarded; this is a component-level model, not a longest
 * path in the original cyclic graph. Weights represent inter-component delays.
 */
public final class Condensation {
    private Condensation() { }
    public static Graph build(Graph original, SccResult scc) {
        original.requireDirected(true);
        Map<Long, Integer> maximumWeight = new HashMap<>();
        for (int e = 0; e < original.m(); e++) {
            int u = scc.component()[original.from(e)], v = scc.component()[original.to(e)];
            if (u == v) continue;
            long key = ((long) u << 32) | (v & 0xffffffffL);
            maximumWeight.merge(key, original.weight(e), Math::max);
        }
        // Sorting pairs makes the result independent of HashMap iteration order.
        long[] keys = new long[maximumWeight.size()];
        int i = 0;
        for (long key : maximumWeight.keySet()) keys[i++] = key;
        Arrays.sort(keys);
        Graph dag = new Graph(scc.count(), true, keys.length);
        for (long key : keys) dag.addEdge((int) (key >>> 32), (int) key, maximumWeight.get(key));
        return dag;
    }
    public record CriticalPath(long length, int[] components) { }
    public static CriticalPath criticalPath(Graph dag) {
        int[] order = Kahn.order(dag, new Metrics());
        long[] distance = new long[dag.n()];
        int[] parent = new int[dag.n()];
        Arrays.fill(distance, -DagShortestPath.INF); Arrays.fill(parent, -1);
        for (int v = 0; v < dag.n(); v++) if (dag.inDegree(v) == 0) distance[v] = 0;
        int best = -1;
        for (int v : order) {
            if (best == -1 || distance[v] > distance[best]) best = v;
            for (int a = dag.firstArc(v); a != -1; a = dag.nextArc(a)) {
                int u = dag.arcTo(a);
                long candidate = distance[v] + dag.arcWeight(a);
                if (candidate > distance[u]) { distance[u] = candidate; parent[u] = v; }
            }
        }
        if (best == -1) return new CriticalPath(0, new int[0]);
        int size = 0;
        for (int v = best; v != -1; v = parent[v]) size++;
        int[] path = new int[size];
        for (int v = best; v != -1; v = parent[v]) path[--size] = v;
        return new CriticalPath(distance[best], path);
    }
}
