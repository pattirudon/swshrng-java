package jp.co.pattirudon;

import java.util.stream.LongStream;

import org.junit.Test;

public class RandomUIntSolverTest {
    @Test
    public void testSolve() { // 1.7s
        // long seed = 0x8ecfa20fac133deeL;
        int indexPrimary = 23;
        int indexEndSecondaryExclusive = 16;
        int uintPrimary = 0x9cd94855;
        int uintSecondary = 0x42f20304;
        int s0StartInclusive = 0x56000000;
        int s0EndExclusive = 0x57000000;
        LongStream s = RandomUIntSolver.solve(indexPrimary, indexEndSecondaryExclusive, uintPrimary,
                new int[] { uintSecondary }, s0StartInclusive, s0EndExclusive);
        s.forEach(q -> System.out.printf("%016x%n", q));
    }

    @Test
    public void benchmarkSolve() { // 89.3s
        int indexPrimary = 23;
        int indexEndSecondaryExclusive = 16;
        int uintPrimary = 0x9cd94855;
        int uintSecondary = 0x42f20304;
        LongStream s = RandomUIntSolver.solve(indexPrimary, indexEndSecondaryExclusive, uintPrimary,
                new int[] { uintSecondary });
        s.forEach(q -> System.out.printf("%016x%n", q));
    }
}
