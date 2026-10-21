package kz.aitu.daa.algorithms;

import java.util.Arrays;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;

public final class Kruskal {
    private Kruskal() { }
    public static MstResult run(Graph graph, Metrics metrics) {
        graph.requireDirected(false);
        int m = graph.m();
        int[] sorted = new int[m], buffer = new int[m];
        for (int e = 0; e < m; e++) sorted[e] = e;
        // Own stable bottom-up merge sort: every weight comparison is counted.
        for (long width = 1; width < m; width *= 2) {
            for (long start = 0; start < m; start += 2 * width) {
                int lo = (int) start, mid = (int) Math.min(start + width, m);
                int hi = (int) Math.min(start + 2 * width, m), a = lo, b = mid;
                for (int k = lo; k < hi; k++) {
                    if (a < mid && (b >= hi || metrics.compareWeights(
                            graph.weight(sorted[a]), graph.weight(sorted[b])) <= 0))
                        buffer[k] = sorted[a++];
                    else buffer[k] = sorted[b++];
                }
            }
            int[] temp = sorted; sorted = buffer; buffer = temp;
        }
        Dsu dsu = new Dsu(graph.n(), metrics);
        int[] chosen = new int[Math.max(0, graph.n() - 1)];
        int count = 0;
        long total = 0;
        for (int e : sorted) {
            if (dsu.components() <= 1) break;
            metrics.edgeScans++;
            // union performs find(u), find(v), and joins only distinct roots.
            if (dsu.union(graph.from(e), graph.to(e))) {
                chosen[count++] = e;
                total += graph.weight(e);
            }
        }
        return new MstResult(total, Arrays.copyOf(chosen, count), dsu.components());
    }
}
