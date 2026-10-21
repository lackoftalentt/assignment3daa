package kz.aitu.daa;

import java.nio.file.Path;
import java.util.Arrays;
import kz.aitu.daa.algorithms.*;
import kz.aitu.daa.benchmark.Benchmark;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;

public final class Main {
    private Main() { }
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("benchmark")) {
            Benchmark.run(args.length > 1 ? Path.of(args[1]) : Path.of("results"));
            return;
        }
        if (args.length == 2 && args[0].equals("demo")) {
            demo(Graph.read(Path.of(args[1])));
            return;
        }
        System.out.println("Beibarys Balykbayev | SE-2516 | DAA Assignment 3");
        System.out.println("Usage: java -jar target/daa-assignment3-graphs-1.0.jar benchmark [results-directory]");
        System.out.println("       java -jar target/daa-assignment3-graphs-1.0.jar demo data/dag.txt");
    }
    private static void demo(Graph graph) {
        System.out.printf("Graph: n=%d m=%d %s%n", graph.n(), graph.m(), graph.directed() ? "directed" : "undirected");
        if (!graph.directed()) {
            MstResult k = Kruskal.run(graph, new Metrics()), p = Prim.run(graph, new Metrics());
            if (k.totalWeight() != p.totalWeight()) throw new IllegalStateException("MST disagreement");
            System.out.printf("Kruskal = Prim = %d; components=%d; selected edges=%d%n", k.totalWeight(), k.components(), k.edges().length);
            for (int e : k.edges()) System.out.printf("  %d -- %d (%d)%n", graph.from(e), graph.to(e), graph.weight(e));
        } else {
            Metrics km = new Metrics(), tm = new Metrics();
            SccResult scc = Kosaraju.run(graph, km), tarjan = Tarjan.run(graph, tm);
            if (scc.count() != tarjan.count()) throw new IllegalStateException("SCC disagreement");
            System.out.printf("SCCs=%d; labels=%s; scans Kosaraju=%d Tarjan=%d%n", scc.count(), Arrays.toString(scc.component()), km.edgeScans, tm.edgeScans);
            Graph condensation = Condensation.build(graph, scc);
            var critical = Condensation.criticalPath(condensation);
            System.out.printf("Condensation critical path: length=%d; component IDs=%s%n", critical.length(), Arrays.toString(critical.components()));
            try {
                int[] order = Kahn.order(graph, new Metrics());
                System.out.println("Topological order: " + Arrays.toString(order));
                var paths = DagShortestPath.run(graph, new Metrics());
                for (int v = 0; v < graph.n(); v++) System.out.printf("  v=%d distance=%d path=%s%n", v, paths.distance()[v], Arrays.toString(paths.pathTo(v)));
            } catch (IllegalArgumentException cycle) { System.out.println(cycle.getMessage()); }
        }
    }
}
