package kz.aitu.daa;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import kz.aitu.daa.algorithms.*;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GraphTest {
    @TempDir Path temporary;
    @Test void badInputsAreRejected() throws Exception {
        for (String input : new String[]{"", "2 1 strange\n0 1 5\n", "2 1 directed\n", "2 1 directed\n0 2 3\n", "2 0 directed\n0 1 2\n", "-1 0 directed\n", "2 -1 directed\n"}) {
            Path file = temporary.resolve("bad.txt"); Files.writeString(file, input);
            assertThrows(IllegalArgumentException.class, () -> Graph.read(file));
        }
    }
    @Test void graphDirectionIsCheckedAndResizingPreservesEdges() {
        Graph directed = new Graph(3, true, 0);
        for (int i = 0; i < 100; i++) directed.addEdge(0, 1, i);
        assertEquals(100, directed.m()); assertEquals(100, directed.inDegree(1));
        Graph reverse = directed.reversed(); assertEquals(100, reverse.inDegree(0));
        assertThrows(IllegalArgumentException.class, () -> Kruskal.run(directed, new Metrics()));
        assertThrows(IllegalArgumentException.class, () -> Prim.run(directed, new Metrics()));
        Graph undirected = new Graph(1, false);
        assertThrows(IllegalArgumentException.class, () -> Kosaraju.run(undirected, new Metrics()));
        assertThrows(IllegalArgumentException.class, () -> Kahn.order(undirected, new Metrics()));
        assertThrows(IllegalArgumentException.class, () -> undirected.addEdge(-1, 0, 1));
    }
}
