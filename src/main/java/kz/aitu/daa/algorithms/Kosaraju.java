package kz.aitu.daa.algorithms;

import java.util.Arrays;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;

public final class Kosaraju {
    private Kosaraju() { }
    public static SccResult run(Graph graph, Metrics metrics) {
        graph.requireDirected(true);
        int n = graph.n(), finished = 0;
        boolean[] visited = new boolean[n];
        int[] order = new int[n], stack = new int[n], cursor = new int[n];
        for (int root = 0; root < n; root++) {
            if (visited[root]) continue;
            int depth = 1;
            stack[0] = root; cursor[0] = graph.firstArc(root); visited[root] = true;
            metrics.depth(depth);
            while (depth > 0) {
                int top = depth - 1, a = cursor[top];
                if (a == -1) { order[finished++] = stack[top]; depth--; continue; }
                cursor[top] = graph.nextArc(a);
                metrics.edgeScans++;
                int v = graph.arcTo(a);
                if (!visited[v]) {
                    visited[v] = true;
                    stack[depth] = v; cursor[depth] = graph.firstArc(v); depth++;
                    metrics.depth(depth);
                }
            }
        }
        // Transpose construction is timed but not a DFS edge scan. Total time stays O(V+E).
        Graph reverse = graph.reversed();
        int[] component = new int[n];
        Arrays.fill(component, -1);
        int count = 0;
        for (int i = n - 1; i >= 0; i--) {
            int root = order[i];
            if (component[root] != -1) continue;
            int depth = 1;
            stack[0] = root; cursor[0] = reverse.firstArc(root); component[root] = count;
            metrics.depth(depth);
            while (depth > 0) {
                int top = depth - 1, a = cursor[top];
                if (a == -1) { depth--; continue; }
                cursor[top] = reverse.nextArc(a);
                metrics.edgeScans++;
                int v = reverse.arcTo(a);
                if (component[v] == -1) {
                    component[v] = count;
                    stack[depth] = v; cursor[depth] = reverse.firstArc(v); depth++;
                    metrics.depth(depth);
                }
            }
            count++;
        }
        return new SccResult(count, component);
    }
}
