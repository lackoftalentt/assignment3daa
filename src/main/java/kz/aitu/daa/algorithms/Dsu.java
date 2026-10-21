package kz.aitu.daa.algorithms;

import kz.aitu.daa.metrics.Metrics;

/** Union by size plus iterative full path compression. */
public final class Dsu {
    private int[] parent, size;
    private int components;
    private final Metrics metrics;

    public Dsu(int n, Metrics metrics) { this.metrics = metrics; makeSet(n); }
    public void makeSet(int n) {
        if (n < 0) throw new IllegalArgumentException("Negative set count");
        parent = new int[n]; size = new int[n]; components = n;
        for (int i = 0; i < n; i++) { parent[i] = i; size[i] = 1; }
    }
    public int find(int x) {
        if (x < 0 || x >= parent.length) throw new IllegalArgumentException("Invalid element: " + x);
        metrics.findCalls++;
        int root = x;
        while (parent[root] != root) root = parent[root];
        while (parent[x] != x) {
            int next = parent[x]; parent[x] = root; x = next;
        }
        return root;
    }
    public boolean union(int x, int y) {
        int a = find(x), b = find(y);
        if (a == b) return false;
        if (size[a] < size[b]) { int temp = a; a = b; b = temp; }
        parent[b] = a; size[a] += size[b]; components--;
        metrics.successfulUnions++;
        return true;
    }
    public int components() { return components; }
}
