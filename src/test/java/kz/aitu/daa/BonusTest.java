package kz.aitu.daa;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.Random;
import kz.aitu.daa.algorithms.*;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

class BonusTest {
    @Test void condensationIsDagAndRetainsMaximumParallelDependency() {
        Graph graph = new Graph(5, true);
        graph.addEdge(0, 1, 100); graph.addEdge(1, 0, 200); // SCC A
        graph.addEdge(1, 2, 3); graph.addEdge(0, 2, 7); // A -> B, keep 7
        graph.addEdge(2, 3, 4); graph.addEdge(3, 4, 9); graph.addEdge(4, 3, 9); // B -> C
        Graph dag = Condensation.build(graph, Kosaraju.run(graph, new Metrics()));
        assertEquals(3, dag.n()); assertEquals(2, dag.m());
        assertTrue(Kahn.isValidOrder(dag, Kahn.order(dag, new Metrics())));
        assertEquals(11, Condensation.criticalPath(dag).length());
        assertEquals(3, Condensation.criticalPath(dag).components().length);
    }
    @Test void randomCondensationsAreAcyclicAndLongestPathMatchesEnumeration() {
        Random r = new Random(74);
        for (int trial = 0; trial < 50; trial++) {
            Graph graph = new Graph(8, true);
            for (int i = 0; i < 12; i++) graph.addEdge(r.nextInt(8), r.nextInt(8), r.nextInt(21) - 10);
            Graph dag = Condensation.build(graph, Tarjan.run(graph, new Metrics()));
            assertTrue(Kahn.isValidOrder(dag, Kahn.order(dag, new Metrics())));
            long best = 0;
            for (int v = 0; v < dag.n(); v++) if (dag.inDegree(v) == 0) best = Math.max(best, exhaustiveLongest(dag, v, 0));
            assertEquals(best, Condensation.criticalPath(dag).length());
        }
    }
    // Test-only recursion: at most eight vertices; the production algorithms are iterative.
    private static long exhaustiveLongest(Graph dag, int v, long length) {
        long best = length;
        for (int a = dag.firstArc(v); a != -1; a = dag.nextArc(a))
            best = Math.max(best, exhaustiveLongest(dag, dag.arcTo(a), length + dag.arcWeight(a)));
        return best;
    }
    @Test void suppliedCycleCondensesAndEmptyCriticalPath() throws Exception {
        Graph original = Graph.read(Path.of("data/cyclic.txt"));
        Graph dag = Condensation.build(original, Kosaraju.run(original, new Metrics()));
        assertEquals(4, dag.n()); assertEquals(2, dag.m());
        assertEquals(4, Condensation.criticalPath(dag).length());
        assertEquals(0, Condensation.criticalPath(new Graph(0, true)).components().length);
    }
}
