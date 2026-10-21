package kz.aitu.daa.benchmark;

import java.util.Random;
import kz.aitu.daa.graph.Graph;

/** Literal assignment generator. Every method starts from a fresh java.util.Random(42). */
public final class GraphGenerator {
    private GraphGenerator() { }
    public enum Kind { UNDIRECTED, DIRECTED, DAG }
    public static Graph generate(int n, int m, Kind kind) {
        if (n < 2 || m < 0 || (kind == Kind.UNDIRECTED && m < n - 1))
            throw new IllegalArgumentException("Generator needs n >= 2 and enough tree edges");
        Random random = new Random(42);
        Graph graph = new Graph(n, kind != Kind.UNDIRECTED, m);
        int[] permutation = null;
        if (kind != Kind.DIRECTED) {
            permutation = new int[n];
            for (int i = 0; i < n; i++) permutation[i] = i;
            for (int i = n - 1; i > 0; i--) {
                int j = random.nextInt(i + 1), temp = permutation[i];
                permutation[i] = permutation[j]; permutation[j] = temp;
            }
        }
        if (kind == Kind.UNDIRECTED) {
            for (int i = 1; i < n; i++) {
                int a = permutation[random.nextInt(i)], b = permutation[i];
                graph.addEdge(a, b, 1 + random.nextInt(1000));
            }
        }
        while (graph.m() < m) {
            int a = random.nextInt(n), b;
            do { b = random.nextInt(n); } while (b == a);
            if (kind == Kind.DAG && permutation[a] > permutation[b]) {
                int temp = a; a = b; b = temp;
            }
            graph.addEdge(a, b, 1 + random.nextInt(1000));
        }
        return graph;
    }
}
