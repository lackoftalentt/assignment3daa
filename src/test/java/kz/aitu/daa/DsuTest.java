package kz.aitu.daa;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Random;
import kz.aitu.daa.algorithms.Dsu;
import kz.aitu.daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

class DsuTest {
    @Test void randomUnionsAgreeWithRelabelingReference() {
        int n = 100;
        int[] label = new int[n];
        for (int i = 0; i < n; i++) label[i] = i;
        Metrics metrics = new Metrics();
        Dsu dsu = new Dsu(n, metrics);
        Random random = new Random(123);
        int components = n;
        for (int step = 0; step < 1000; step++) {
            int a = random.nextInt(n), b = random.nextInt(n);
            boolean different = label[a] != label[b];
            assertEquals(different, dsu.union(a, b));
            if (different) {
                int old = label[b], replacement = label[a];
                for (int i = 0; i < n; i++) if (label[i] == old) label[i] = replacement;
                components--;
            }
            assertEquals(components, dsu.components());
            for (int i = 0; i < n; i++)
                assertEquals(label[a] == label[i], dsu.find(a) == dsu.find(i));
        }
        assertEquals(n - components, metrics.successfulUnions);
        assertEquals(202000, metrics.findCalls);
    }
    @Test void resetAndBoundaryCases() {
        Dsu dsu = new Dsu(0, new Metrics());
        assertEquals(0, dsu.components());
        assertThrows(IllegalArgumentException.class, () -> dsu.find(0));
        dsu.makeSet(2);
        assertTrue(dsu.union(0, 1));
        assertFalse(dsu.union(1, 0));
        dsu.makeSet(2);
        assertEquals(2, dsu.components());
        assertNotEquals(dsu.find(0), dsu.find(1));
    }
}
