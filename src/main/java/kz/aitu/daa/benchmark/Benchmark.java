package kz.aitu.daa.benchmark;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Supplier;
import kz.aitu.daa.algorithms.*;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;
import static kz.aitu.daa.benchmark.GraphGenerator.Kind.*;

/** 10 (size,density) cases x 5 algorithms = 50 CSV rows; bonus results are separate. */
public final class Benchmark {
    private Benchmark() { }
    private static final int[] SIZES = {1000, 3162, 10000, 31623, 100000};
    private static final long[][] REFERENCES = {
        {147970,27,396274}, {30363,1,252411}, {468911,137,1271911}, {97589,1,723157},
        {1486139,373,3941828}, {309550,1,2381342}, {4715515,1188,12551891}, {968517,1,7376612},
        {15012094,3923,40096539}, {3056681,1,23512087}
    };
    private record Outcome(long result, Metrics metrics, Object detail) { }
    private record Measurement(Outcome outcome, double medianMs) { }
    public static void run(Path directory) throws Exception {
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("environment.txt"), "Author: Beibarys Balykbayev, SE-2516\n"
                + "UTC: " + Instant.now() + "\nJava: " + System.getProperty("java.runtime.version")
                + "\nVM: " + System.getProperty("java.vm.name") + "\nOS: " + System.getProperty("os.name")
                + " " + System.getProperty("os.arch") + "\nWarmups: 1; measured repetitions: 5; time statistic: median\n"
                + "Fresh Random(42) for every kind/size/density; generation/validation excluded from timing.\n");
        // Write temporary files: a failed reference check must not replace a successful CSV.
        Path temporary = directory.resolve("results.csv.tmp"), bonusTemporary = directory.resolve("bonus.csv.tmp");
        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(temporary));
             PrintWriter bonus = new PrintWriter(Files.newBufferedWriter(bonusTemporary))) {
            String header = "algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result";
            csv.println(header); bonus.println(header);
            int caseIndex = 0;
            for (int n : SIZES) for (int density : new int[]{4, 20}) {
                int m = n * density;
                String label = density == 4 ? "sparse" : "dense";
                long[] expected = REFERENCES[caseIndex++];
                long mst = mstCase(csv, n, m, label, expected[0]);
                long scc = sccCase(csv, bonus, n, m, label, expected[1]);
                long sum = dagCase(csv, n, m, label, expected[2]);
                System.out.printf(Locale.ROOT, "%d / %d | MST=%d | SCC=%d | DAG sum=%d | OK%n", n, m, mst, scc, sum);
                csv.flush(); bonus.flush();
                if (csv.checkError() || bonus.checkError()) throw new IllegalStateException("Could not write benchmark CSV");
            }
        }
        Files.move(temporary, directory.resolve("results.csv"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.move(bonusTemporary, directory.resolve("bonus.csv"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        System.out.println("PASS: all 30 reference values, MST agreement, valid orders, SCC partitions, exact scan counts; 50 main rows + 10 bonus rows.");
    }
    private static long mstCase(PrintWriter csv, int n, int m, String label, long expected) {
        Graph graph = GraphGenerator.generate(n, m, UNDIRECTED);
        Measurement k = measure(() -> {
            Metrics metrics = new Metrics(); MstResult result = Kruskal.run(graph, metrics);
            return new Outcome(result.totalWeight(), metrics, result);
        });
        Measurement p = measure(() -> {
            Metrics metrics = new Metrics(); MstResult result = Prim.run(graph, metrics);
            return new Outcome(result.totalWeight(), metrics, result);
        });
        require(k.outcome.result == p.outcome.result, "Kruskal/Prim disagreement");
        require(k.outcome.result == expected, "MST reference mismatch: " + k.outcome.result + " != " + expected);
        for (Measurement measurement : new Measurement[]{k, p}) {
            MstResult forest = (MstResult) measurement.outcome.detail;
            require(forest.components() == 1 && forest.edges().length == n - 1, "Not a spanning tree");
        }
        require(p.outcome.metrics.edgeScans == 2L * m, "Prim must scan both adjacency arcs");
        row(csv, "Kruskal", "undirected_" + label, n, m, k);
        row(csv, "Prim", "undirected_" + label, n, m, p);
        return k.outcome.result;
    }
    private static long sccCase(PrintWriter csv, PrintWriter bonus, int n, int m, String label, long expected) {
        Graph graph = GraphGenerator.generate(n, m, DIRECTED);
        Measurement k = measure(() -> {
            Metrics metrics = new Metrics(); SccResult result = Kosaraju.run(graph, metrics);
            return new Outcome(result.count(), metrics, result);
        });
        Measurement t = measure(() -> {
            Metrics metrics = new Metrics(); SccResult result = Tarjan.run(graph, metrics);
            return new Outcome(result.count(), metrics, result);
        });
        require(k.outcome.result == expected, "SCC reference mismatch: " + k.outcome.result + " != " + expected);
        require(samePartition((SccResult) k.outcome.detail, (SccResult) t.outcome.detail), "Tarjan/Kosaraju partition mismatch");
        require(k.outcome.metrics.edgeScans == 2L * m && t.outcome.metrics.edgeScans == m, "SCC scan count mismatch");
        row(csv, "Kosaraju", "directed_" + label, n, m, k);
        row(bonus, "Tarjan", "directed_" + label, n, m, t);
        return k.outcome.result;
    }
    private static long dagCase(PrintWriter csv, int n, int m, String label, long expected) {
        Graph graph = GraphGenerator.generate(n, m, DAG);
        Measurement k = measure(() -> {
            Metrics metrics = new Metrics(); int[] order = Kahn.order(graph, metrics);
            return new Outcome(order.length, metrics, order);
        });
        require(Kahn.isValidOrder(graph, (int[]) k.outcome.detail), "Invalid topological order");
        Measurement p = measure(() -> {
            Metrics metrics = new Metrics(); var result = DagShortestPath.run(graph, metrics);
            return new Outcome(result.sum(), metrics, result);
        });
        require(p.outcome.result == expected, "DAG reference mismatch: " + p.outcome.result + " != " + expected);
        require(k.outcome.metrics.edgeScans == m && p.outcome.metrics.edgeScans == m, "DAG scan count mismatch");
        row(csv, "Kahn", "dag_" + label, n, m, k);
        row(csv, "DagShortestPath", "dag_" + label, n, m, p);
        return p.outcome.result;
    }
    private static Measurement measure(Supplier<Outcome> action) {
        Outcome baseline = action.get(); // one warmup; counters must agree across all repetitions
        double[] times = new double[5];
        Outcome last = baseline;
        for (int run = 0; run < times.length; run++) {
            long start = System.nanoTime();
            last = action.get();
            times[run] = (System.nanoTime() - start) / 1_000_000.0;
            require(last.result == baseline.result && last.metrics.sameCounts(baseline.metrics), "Nondeterministic benchmark");
        }
        Arrays.sort(times);
        return new Measurement(last, times[2]);
    }
    public static boolean samePartition(SccResult a, SccResult b) {
        if (a.count() != b.count() || a.component().length != b.component().length) return false;
        int[] forward = new int[a.count()], backward = new int[b.count()];
        Arrays.fill(forward, -1); Arrays.fill(backward, -1);
        for (int v = 0; v < a.component().length; v++) {
            int x = a.component()[v], y = b.component()[v];
            if ((forward[x] != -1 && forward[x] != y) || (backward[y] != -1 && backward[y] != x)) return false;
            forward[x] = y; backward[y] = x;
        }
        return true;
    }
    private static void row(PrintWriter csv, String algorithm, String graph, int n, int m, Measurement value) {
        Metrics c = value.outcome.metrics;
        csv.printf(Locale.ROOT, "%s,%s,%d,%d,%.6f,%d,%d,%d,%d,%d%n", algorithm, graph, n, m, value.medianMs,
                c.dsuOps(), c.edgeScans, c.comparisons, c.maxDepth, value.outcome.result);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
