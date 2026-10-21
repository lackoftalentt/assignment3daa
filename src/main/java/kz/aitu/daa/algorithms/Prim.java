package kz.aitu.daa.algorithms;

import java.util.Arrays;
import java.util.PriorityQueue;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;

/** Lazy edge-frontier Prim. Restart at each unvisited root to return a forest. */
public final class Prim {
    private Prim() { }
    private record Candidate(int vertex, int edge, int weight) { }
    public static MstResult run(Graph graph, Metrics metrics) {
        graph.requireDirected(false);
        boolean[] visited = new boolean[graph.n()];
        int[] chosen = new int[Math.max(0, graph.n() - 1)];
        var queue = new PriorityQueue<Candidate>((a, b) -> {
            int cmp = metrics.compareWeights(a.weight, b.weight);
            if (cmp != 0) return cmp;
            cmp = Integer.compare(a.vertex, b.vertex);
            return cmp != 0 ? cmp : Integer.compare(a.edge, b.edge);
        });
        long total = 0;
        int count = 0, components = 0;
        for (int root = 0; root < graph.n(); root++) {
            if (visited[root]) continue;
            components++;
            queue.add(new Candidate(root, -1, 0));
            while (!queue.isEmpty()) {
                Candidate candidate = queue.remove();
                int v = candidate.vertex;
                if (visited[v]) continue;
                visited[v] = true;
                if (candidate.edge != -1) {
                    chosen[count++] = candidate.edge;
                    total += candidate.weight;
                }
                for (int a = graph.firstArc(v); a != -1; a = graph.nextArc(a)) {
                    metrics.edgeScans++;
                    int u = graph.arcTo(a);
                    if (!visited[u]) queue.add(new Candidate(u, graph.arcEdge(a), graph.arcWeight(a)));
                }
            }
        }
        return new MstResult(total, Arrays.copyOf(chosen, count), components);
    }
}
