package kz.aitu.daa.algorithms;

/** IDs are arbitrary: compare partitions, not the numeric labels themselves. */
public record SccResult(int count, int[] component) { }
