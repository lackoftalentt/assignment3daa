package kz.aitu.daa;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import kz.aitu.daa.algorithms.*;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

class PathsTest {
    @Test void randomDagsWithNegativeWeightsAgreeWithBellmanFord() {
        Random r = new Random(441);
        for (int trial = 0; trial < 100; trial++) {
            int n = 2 + r.nextInt(45);
            Graph graph = new Graph(n, true);
            int[] rank = new int[n];
            for (int i = 0; i < n; i++) rank[i] = i;
            for (int i = n - 1; i > 0; i--) {
                int j = r.nextInt(i + 1), temp = rank[i]; rank[i] = rank[j]; rank[j] = temp;
            }
            for (int i = 0; i < 4 * n; i++) {
                int a = r.nextInt(n), b = r.nextInt(n);
                if (a == b) continue;
                if (rank[a] > rank[b]) { int temp = a; a = b; b = temp; }
                graph.addEdge(a, b, r.nextInt(101) - 50);
            }
            Metrics metrics = new Metrics();
            var result = DagShortestPath.run(graph, metrics);
            assertArrayEquals(bellmanFord(graph), result.distance());
            assertEquals(graph.m(), metrics.edgeScans);
            assertTrue(Kahn.isValidOrder(graph, Kahn.order(graph, new Metrics())));
            for (int v = 0; v < n; v++) {
                int[] path = result.pathTo(v);
                assertEquals(v, path[path.length - 1]);
                assertEquals(0, graph.inDegree(path[0]));
                long weight = 0;
                for (int i = 1; i < path.length; i++) {
                    int a = path[i - 1], b = path[i];
                    // With parallel edges the recorded predecessor uses the cheapest usable edge.
                    int minimum = Integer.MAX_VALUE;
                    for (int e = graph.firstArc(a); e != -1; e = graph.nextArc(e))
                        if (graph.arcTo(e) == b) minimum = Math.min(minimum, graph.arcWeight(e));
                    assertNotEquals(Integer.MAX_VALUE, minimum);
                    weight += minimum;
                }
                assertEquals(result.distance()[v], weight);
            }
        }
    }
    private static long[] bellmanFord(Graph graph) {
        long[] distance = new long[graph.n()];
        Arrays.fill(distance, DagShortestPath.INF);
        for (int v = 0; v < graph.n(); v++) if (graph.inDegree(v) == 0) distance[v] = 0;
        for (int pass = 1; pass < graph.n(); pass++) {
            boolean changed = false;
            for (int e = 0; e < graph.m(); e++) {
                int a = graph.from(e), b = graph.to(e);
                if (distance[a] != DagShortestPath.INF && distance[a] + graph.weight(e) < distance[b]) {
                    distance[b] = distance[a] + graph.weight(e); changed = true;
                }
            }
            if (!changed) break;
        }
        return distance;
    }
    @Test void sampleHasTwoSourcesOneIsolatedVertexAndAnActualNegativePath() throws Exception {
        Graph graph = Graph.read(Path.of("data/dag.txt"));
        var result = DagShortestPath.run(graph, new Metrics());
        assertArrayEquals(new long[]{0, 0, -2, -1, -7, -5, 0}, result.distance());
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, result.pathTo(5));
        assertArrayEquals(new int[]{6}, result.pathTo(6));
        assertEquals(-15, result.sum());
        assertFalse(Kahn.isValidOrder(graph, new int[]{0, 0, 1, 2, 3, 4, 5}));
        assertFalse(Kahn.isValidOrder(graph, new int[]{0, 1}));
        assertFalse(Kahn.isValidOrder(graph, new int[]{0, 1, 2, 3, 4, 5, 7}));
        assertFalse(Kahn.isValidOrder(graph, new int[]{5, 4, 3, 2, 1, 0, 6}));
    }
    @Test void cyclesAndSelfLoopsAreRejected() throws Exception {
        Graph cyclic = Graph.read(Path.of("data/cyclic.txt"));
        assertThrows(IllegalArgumentException.class, () -> Kahn.order(cyclic, new Metrics()));
        assertThrows(IllegalArgumentException.class, () -> DagShortestPath.run(cyclic, new Metrics()));
        Graph loop = new Graph(1, true); loop.addEdge(0, 0, -1);
        assertThrows(IllegalArgumentException.class, () -> Kahn.order(loop, new Metrics()));
    }
    @Test void distancesUseLongAndEmptyDagIsValid() {
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, Integer.MAX_VALUE); graph.addEdge(1, 2, Integer.MAX_VALUE);
        assertEquals(4294967294L, DagShortestPath.run(graph, new Metrics()).distance()[2]);
        Graph empty = new Graph(0, true);
        assertTrue(Kahn.isValidOrder(empty, Kahn.order(empty, new Metrics())));
        assertEquals(0, DagShortestPath.run(empty, new Metrics()).sum());
    }
}
