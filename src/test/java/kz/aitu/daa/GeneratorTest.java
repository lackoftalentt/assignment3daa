package kz.aitu.daa;

import static org.junit.jupiter.api.Assertions.*;
import kz.aitu.daa.algorithms.*;
import kz.aitu.daa.benchmark.GraphGenerator;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;
import org.junit.jupiter.api.Test;
import static kz.aitu.daa.benchmark.GraphGenerator.Kind.*;

class GeneratorTest {
    @Test void twoSmallestDensityCasesMatchPublishedReferences() {
        int[] densities = {4, 20};
        long[] mst = {147970, 30363}, scc = {27, 1}, sums = {396274, 252411};
        for (int i = 0; i < 2; i++) {
            Graph undirected = GraphGenerator.generate(1000, 1000 * densities[i], UNDIRECTED);
            assertEquals(mst[i], Kruskal.run(undirected, new Metrics()).totalWeight());
            assertEquals(mst[i], Prim.run(undirected, new Metrics()).totalWeight());
            Graph directed = GraphGenerator.generate(1000, 1000 * densities[i], DIRECTED);
            assertEquals(scc[i], Kosaraju.run(directed, new Metrics()).count());
            Graph dag = GraphGenerator.generate(1000, 1000 * densities[i], DAG);
            assertEquals(sums[i], DagShortestPath.run(dag, new Metrics()).sum());
        }
    }
}
