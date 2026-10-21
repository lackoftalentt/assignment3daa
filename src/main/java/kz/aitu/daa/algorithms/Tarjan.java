package kz.aitu.daa.algorithms;

import java.util.Arrays;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;

/** Bonus A: iterative Tarjan, with separate DFS frames and the active SCC stack. */
public final class Tarjan {
    private Tarjan() { }
    public static SccResult run(Graph graph, Metrics metrics) {
        graph.requireDirected(true);
        int n = graph.n(), clock = 0, count = 0, activeSize = 0;
        int[] index = new int[n], low = new int[n], component = new int[n];
        int[] frames = new int[n], cursor = new int[n], active = new int[n];
        boolean[] onStack = new boolean[n];
        Arrays.fill(index, -1); Arrays.fill(component, -1);
        for (int root = 0; root < n; root++) {
            if (index[root] != -1) continue;
            int depth = 1;
            frames[0] = root; cursor[0] = graph.firstArc(root);
            index[root] = low[root] = clock++;
            active[activeSize++] = root; onStack[root] = true;
            metrics.depth(Math.max(depth, activeSize));
            while (depth > 0) {
                int top = depth - 1, v = frames[top], a = cursor[top];
                if (a != -1) {
                    cursor[top] = graph.nextArc(a);
                    metrics.edgeScans++;
                    int u = graph.arcTo(a);
                    if (index[u] == -1) {
                        index[u] = low[u] = clock++;
                        active[activeSize++] = u; onStack[u] = true;
                        frames[depth] = u; cursor[depth] = graph.firstArc(u); depth++;
                        metrics.depth(Math.max(depth, activeSize));
                    } else if (onStack[u]) low[v] = Math.min(low[v], index[u]);
                } else {
                    depth--;
                    if (low[v] == index[v]) {
                        int u;
                        do {
                            u = active[--activeSize]; onStack[u] = false; component[u] = count;
                        } while (u != v);
                        count++;
                    }
                    if (depth > 0) {
                        int parent = frames[depth - 1];
                        low[parent] = Math.min(low[parent], low[v]);
                    }
                }
            }
        }
        return new SccResult(count, component);
    }
}
