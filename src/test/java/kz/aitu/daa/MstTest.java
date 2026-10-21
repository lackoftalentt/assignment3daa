package kz.aitu.daa;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.Random;
import kz.aitu.daa.algorithms.*;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

class MstTest {
    @Test void oneHundredConnectedRandomGraphs() {
        Random r = new Random(250);
        for (int trial = 0; trial < 100; trial++) {
            int n = 2 + r.nextInt(50);
            Graph g = new Graph(n, false);
            for (int v = 1; v < n; v++) g.addEdge(r.nextInt(v), v, r.nextInt(101) - 50);
            for (int e = 0; e < 5 * n; e++) g.addEdge(r.nextInt(n), r.nextInt(n), r.nextInt(101) - 50);
            MstResult k = Kruskal.run(g, new Metrics()), p = Prim.run(g, new Metrics());
            assertEquals(k.totalWeight(), p.totalWeight());
            assertForest(g, k); assertForest(g, p);
            assertEquals(n - 1, k.edges().length);
            assertEquals(n - 1, p.edges().length);
        }
    }
    @Test void tinyGraphsAgreeWithExhaustiveSpanningTreeSearch() {
        Random r = new Random(88);
        for (int trial = 0; trial < 40; trial++) {
            Graph g = new Graph(5, false);
            for (int v = 1; v < 5; v++) g.addEdge(v - 1, v, r.nextInt(21) - 10);
            for (int e = 0; e < 5; e++) g.addEdge(r.nextInt(5), r.nextInt(5), r.nextInt(21) - 10);
            long best = Long.MAX_VALUE;
            for (int mask = 0; mask < (1 << g.m()); mask++) {
                if (Integer.bitCount(mask) != g.n() - 1) continue;
                // Independent connectivity check with a tiny adjacency matrix, no DSU.
                boolean[][] reach = new boolean[5][5];
                for (int i = 0; i < 5; i++) reach[i][i] = true;
                long weight = 0;
                for (int e = 0; e < g.m(); e++) if ((mask & (1 << e)) != 0) {
                    reach[g.from(e)][g.to(e)] = reach[g.to(e)][g.from(e)] = true;
                    weight += g.weight(e);
                }
                for (int k = 0; k < 5; k++) for (int i = 0; i < 5; i++) for (int j = 0; j < 5; j++)
                    reach[i][j] |= reach[i][k] && reach[k][j];
                boolean connected = true;
                for (int v = 0; v < 5; v++) connected &= reach[0][v];
                if (connected) best = Math.min(best, weight);
            }
            assertEquals(best, Kruskal.run(g, new Metrics()).totalWeight());
            assertEquals(best, Prim.run(g, new Metrics()).totalWeight());
        }
    }
    @Test void suppliedGraphsAndEmptyForest() throws Exception {
        Graph connected = Graph.read(Path.of("data/connected.txt"));
        assertEquals(10, Kruskal.run(connected, new Metrics()).totalWeight());
        Graph g = Graph.read(Path.of("data/disconnected.txt"));
        for (MstResult result : new MstResult[]{Kruskal.run(g, new Metrics()), Prim.run(g, new Metrics())}) {
            assertEquals(3, result.totalWeight());
            assertEquals(4, result.components());
            assertEquals(3, result.edges().length);
            assertForest(g, result);
        }
        Graph empty = new Graph(0, false);
        assertEquals(0, Kruskal.run(empty, new Metrics()).components());
        assertEquals(0, Prim.run(empty, new Metrics()).components());
    }
    private static void assertForest(Graph g, MstResult result) {
        Dsu dsu = new Dsu(g.n(), new Metrics());
        long total = 0;
        for (int e : result.edges()) {
            assertTrue(dsu.union(g.from(e), g.to(e)), "Returned a cycle");
            total += g.weight(e);
        }
        assertEquals(result.totalWeight(), total);
        assertEquals(result.components(), dsu.components());
        assertEquals(g.n() - result.components(), result.edges().length);
    }
}
