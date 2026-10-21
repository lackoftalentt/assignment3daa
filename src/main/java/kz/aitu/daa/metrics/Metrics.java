package kz.aitu.daa.metrics;

/** Counters are updated at the operation, never reconstructed from a complexity formula. */
public final class Metrics {
    public long findCalls;
    public long successfulUnions;
    public long edgeScans;
    public long comparisons;
    public int maxDepth;

    public long dsuOps() { return findCalls + successfulUnions; }
    public int compareWeights(int a, int b) {
        comparisons++;
        return Integer.compare(a, b);
    }
    public void depth(int depth) { maxDepth = Math.max(maxDepth, depth); }
    public boolean sameCounts(Metrics other) {
        return findCalls == other.findCalls && successfulUnions == other.successfulUnions
                && edgeScans == other.edgeScans && comparisons == other.comparisons
                && maxDepth == other.maxDepth;
    }
}
