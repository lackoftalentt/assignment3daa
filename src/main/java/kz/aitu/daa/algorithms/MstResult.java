package kz.aitu.daa.algorithms;

/** Selected input edge IDs, forest weight, and number of connected components. */
public record MstResult(long totalWeight, int[] edges, int components) { }
