package kz.aitu.daa.graph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** One adjacency-list type for both graph directions. Primitive arrays keep 2M edges practical.
 * Each input edge has one ID; an undirected edge has two adjacency arcs (including loops).
 */
public final class Graph {
    private final int n;
    private final boolean directed;
    private final int[] head, inDegree;
    private int[] from, to, weight, arcTo, arcNext, arcEdge;
    private int edges, arcs;

    public Graph(int n, boolean directed) { this(n, directed, 4); }
    public Graph(int n, boolean directed, int expectedEdges) {
        if (n < 0 || expectedEdges < 0) throw new IllegalArgumentException("Negative size");
        this.n = n;
        this.directed = directed;
        head = new int[n];
        Arrays.fill(head, -1);
        inDegree = new int[n];
        int capacity = Math.max(1, expectedEdges);
        from = new int[capacity]; to = new int[capacity]; weight = new int[capacity];
        int arcCapacity = Math.multiplyExact(capacity, directed ? 1 : 2);
        arcTo = new int[arcCapacity]; arcNext = new int[arcCapacity]; arcEdge = new int[arcCapacity];
    }
    public int n() { return n; }
    public int m() { return edges; }
    public boolean directed() { return directed; }
    public int firstArc(int v) { return head[v]; }
    public int nextArc(int a) { return arcNext[a]; }
    public int arcTo(int a) { return arcTo[a]; }
    public int arcEdge(int a) { return arcEdge[a]; }
    public int arcWeight(int a) { return weight[arcEdge[a]]; }
    public int from(int e) { return from[e]; }
    public int to(int e) { return to[e]; }
    public int weight(int e) { return weight[e]; }
    public int inDegree(int v) { return inDegree[v]; }

    public void checkVertex(int v) {
        if (v < 0 || v >= n) throw new IllegalArgumentException("Vertex outside 0..n-1: " + v);
    }
    public void requireDirected(boolean expected) {
        if (directed != expected) throw new IllegalArgumentException(expected
                ? "Algorithm requires a directed graph" : "MST requires an undirected graph");
    }
    public void addEdge(int u, int v, int w) {
        checkVertex(u); checkVertex(v);
        if (edges == from.length) {
            int capacity = Math.multiplyExact(from.length, 2);
            from = Arrays.copyOf(from, capacity); to = Arrays.copyOf(to, capacity);
            weight = Arrays.copyOf(weight, capacity);
        }
        from[edges] = u; to[edges] = v; weight[edges] = w;
        addArc(u, v, edges);
        if (!directed) addArc(v, u, edges);
        edges++;
    }
    private void addArc(int u, int v, int edge) {
        if (arcs == arcTo.length) {
            int capacity = Math.multiplyExact(arcTo.length, 2);
            arcTo = Arrays.copyOf(arcTo, capacity); arcNext = Arrays.copyOf(arcNext, capacity);
            arcEdge = Arrays.copyOf(arcEdge, capacity);
        }
        arcTo[arcs] = v; arcEdge[arcs] = edge; arcNext[arcs] = head[u]; head[u] = arcs++;
        inDegree[v]++;
    }
    public Graph reversed() {
        requireDirected(true);
        Graph reverse = new Graph(n, true, edges);
        for (int e = 0; e < edges; e++) reverse.addEdge(to[e], from[e], weight[e]);
        return reverse;
    }
    public static Graph read(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path)) {
            String header = reader.readLine();
            if (header == null) throw new IllegalArgumentException("Empty graph file");
            String[] h = header.trim().split("\\s+");
            if (h.length != 3 || !(h[2].equals("directed") || h[2].equals("undirected")))
                throw new IllegalArgumentException("Expected: n m directed|undirected");
            int n = Integer.parseInt(h[0]), m = Integer.parseInt(h[1]);
            Graph graph = new Graph(n, h[2].equals("directed"), m);
            for (int e = 0; e < m; e++) {
                String line = reader.readLine();
                if (line == null) throw new IllegalArgumentException("Missing edge " + e);
                String[] p = line.trim().split("\\s+");
                if (p.length != 3) throw new IllegalArgumentException("Expected: u v w");
                graph.addEdge(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
            }
            String line;
            while ((line = reader.readLine()) != null)
                if (!line.isBlank()) throw new IllegalArgumentException("Extra data after m edges");
            return graph;
        }
    }
}
