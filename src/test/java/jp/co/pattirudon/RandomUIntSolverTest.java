package jp.co.pattirudon;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.stream.LongStream;

import org.junit.Test;

public class RandomUIntSolverTest {
    @Test
    public void testSolve() { // 1.7s
        // long seed = 0x8ecfa20fac133deeL;
        int indexPrimary = 15;
        int indexStartSecondaryInclusive = 23;
        int indexEndSecondaryExclusive = 24;
        int uintPrimary = 0x42f20304;
        int uintSecondary = 0x9cd94855;
        int s0StartInclusive = 0xc76b6b2e;
        int s0EndExclusive = 0xc76b6b2f;
        RandomUIntSolver solver = new RandomUIntSolver(indexPrimary, indexStartSecondaryInclusive,
                indexEndSecondaryExclusive);
        LongStream s = solver.solve(uintPrimary, new int[] { uintSecondary }, s0StartInclusive, s0EndExclusive);
        long[] a = s.toArray();
        assertArrayEquals(new long[] { 0x8ecfa20fac133deeL }, a);
    }

    @Test
    public void benchmarkSolve() { // 23.6s
        int indexPrimary = 15;
        int indexStartSecondaryInclusive = 23;
        int indexEndSecondaryExclusive = 24;
        int uintPrimary = 0x42f20304;
        int uintSecondary = 0x9cd94855;
        RandomUIntSolver solver = new RandomUIntSolver(indexPrimary, indexStartSecondaryInclusive,
                indexEndSecondaryExclusive);
        assertEquals(2, solver.nullimage.length);
        LongStream s = solver.solve(uintPrimary, new int[] { uintSecondary });
        s.forEach(q -> System.out.printf("%016x%n", q));
    }

    @Test
    public void benchmarkSolve_1() { // 49.9s
        // long seed = 0xb75206104ffd42b1L;
        int indexPrimary = 5000;
        int indexStartSecondaryInclusive = 5030;
        int indexEndSecondaryExclusive = 5039;
        int uintPrimary = 0x759dd729;
        int uintSecondary = 0x0043b866;
        RandomUIntSolver solver = new RandomUIntSolver(indexPrimary, indexStartSecondaryInclusive,
                indexEndSecondaryExclusive);
        assertEquals(2, solver.nullimage.length);
        LongStream s = solver.solve(uintPrimary, new int[] { uintSecondary });
        s.forEach(q -> System.out.printf("%016x%n", q));
    }
}
