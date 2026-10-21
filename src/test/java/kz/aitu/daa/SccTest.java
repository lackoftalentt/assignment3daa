package kz.aitu.daa;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Random;
import kz.aitu.daa.algorithms.*;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

class SccTest {
    @Test void randomGraphsAgreeWithBfsInBothDirections() {
        Random r = new Random(140);
        for (int trial = 0; trial < 45; trial++) {
            int n = trial == 0 ? 200 : 1 + r.nextInt(100);
            Graph graph = new Graph(n, true);
            for (int e = 0; e < n * (trial % 5); e++) graph.addEdge(r.nextInt(n), r.nextInt(n), 1);
            checkAgainstBfs(graph);
        }
    }
    private static void checkAgainstBfs(Graph g) {
        Metrics km = new Metrics(), tm = new Metrics();
        SccResult k = Kosaraju.run(g, km), t = Tarjan.run(g, tm);
        Graph reverse = g.reversed();
        boolean[] counted = new boolean[g.n()];
        int expectedCount = 0;
        for (int u = 0; u < g.n(); u++) {
            boolean[] forward = bfs(g, u), backward = bfs(reverse, u);
            if (!counted[u]) expectedCount++;
            for (int v = 0; v < g.n(); v++) {
                boolean equivalent = forward[v] && backward[v];
                assertEquals(equivalent, k.component()[u] == k.component()[v]);
                assertEquals(equivalent, t.component()[u] == t.component()[v]);
                if (equivalent) counted[v] = true;
            }
        }
        assertEquals(expectedCount, k.count()); assertEquals(expectedCount, t.count());
        assertEquals(2L * g.m(), km.edgeScans); assertEquals(g.m(), tm.edgeScans);
    }
    private static boolean[] bfs(Graph g, int root) {
        boolean[] seen = new boolean[g.n()];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        seen[root] = true; queue.add(root);
        while (!queue.isEmpty()) {
            int v = queue.remove();
            for (int a = g.firstArc(v); a != -1; a = g.nextArc(a)) {
                int u = g.arcTo(a);
                if (!seen[u]) { seen[u] = true; queue.add(u); }
            }
        }
        return seen;
    }
    @Test void providedCyclicGraphIncludesLoopParallelEdgesAndIsolatedVertex() throws Exception {
        Graph graph = Graph.read(Path.of("data/cyclic.txt"));
        assertEquals(4, Kosaraju.run(graph, new Metrics()).count());
        checkAgainstBfs(graph);
    }
    @Test void explicitStacksSurviveOneHundredThousandVertices() {
        Graph path = new Graph(100_000, true, 99_999);
        for (int i = 1; i < path.n(); i++) path.addEdge(i - 1, i, 1);
        Metrics k = new Metrics(), t = new Metrics();
        assertEquals(path.n(), Kosaraju.run(path, k).count());
        assertEquals(path.n(), Tarjan.run(path, t).count());
        assertEquals(100_000, k.maxDepth); assertEquals(100_000, t.maxDepth);
        // Closing the path exercises a huge active SCC, too.
        path.addEdge(path.n() - 1, 0, 1);
        assertEquals(1, Kosaraju.run(path, new Metrics()).count());
        assertEquals(1, Tarjan.run(path, new Metrics()).count());
    }
    @Test void emptyGraph() {
        Graph empty = new Graph(0, true);
        assertEquals(0, Kosaraju.run(empty, new Metrics()).count());
        assertEquals(0, Tarjan.run(empty, new Metrics()).count());
    }
}
