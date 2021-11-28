package jp.co.pattirudon.swshrng;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import jp.co.pattirudon.swshrng.matrices.IntMatrix;
import jp.co.pattirudon.swshrng.matrices.IntMatrix.Enchelon;
import jp.co.pattirudon.swshrng.matrices.LongMatrix;
import jp.co.pattirudon.swshrng.matrices.VerboseLongMatrix;

public class RandomIVSolverTest {
    @Test
    public void testPossibleXors() {
        int summation = 0xaaaaaaaa;
        Set<Integer> xors = RandomIVSolver.possibleXors(summation);
        assertEquals(3524578, xors.size());
    }

    @Test
    public void testLinearIVMatrix() {
        IntMatrix I = RandomIVSolver.linearIVMatrix();
        Enchelon e = I.enchelon();
        int rank = e.rank;
        assertEquals(29, rank);
        for (int k = 0; k < 32; k++) {
            int seed = 1 << k;
            byte[] actual_bit_array = I.multiplyRight(seed);
            int[] actual = new int[actual_bit_array.length / 5];
            for (int j = 0; j < actual.length; j++) {
                int iv = 0;
                for (int l = 0; l < 5; l++) {
                    iv |= actual_bit_array[j * 5 + l] << l;
                }
                actual[j] = iv;
            }
            int[] expected = RandomIVSolver.linearIVs(Integer.toUnsignedLong(seed), 0);
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void testSolve() {
        int[] ivs = { 31, 12, 1, 19, 13, 18 };
        List<Integer> seeds = RandomIVSolver.solve(ivs);
        Set<Integer> seedSet = new TreeSet<>(seeds);
        Set<Integer> expected = Set.of(0xd0f5f22e, 0xa78ab9f4, 0xe3ff170f, 0xbb528cf6);
        assertEquals(seedSet, expected);
    }

    @Test
    public void testRightLinearUIntMatrix() {
        Set<Integer> indices = Set.of(0, 1, 2, 3, 4, 5);
        LongMatrix I = RandomUIntSolver.linearRightUIntMatrix(indices);
        long seed = 0x1566144cade952eaL;
        int[] expected = { 0xade952ea, 0x00000000, 0x151634fe, 0xacc28995, 0xfaf23a3b, 0x15a1b6b7, 0x58d5812e,
                0x1341e57d, 0x01ad2f41, 0x07507889, 0x1074f1d5, 0x6552f420, };
        byte[] y = I.multiplyRight(seed);
        int[] actual = new int[indices.size() * 2];
        for (int j = 0; j < actual.length; j++) {
            int v = 0;
            for (int k = 0; k < 32; k++) {
                v |= y[j * 32 + k] << k;
            }
            actual[j] = v;
        }
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testLeftLinearUIntMatrix() {
        Set<Integer> indices = Set.of(1200);
        LongMatrix I = RandomUIntSolver.linearRightUIntMatrix(indices);
        LongMatrix J = I.generalizedInverse().longMatrix();
        LongMatrix P = RandomUIntSolver.linearLeftUIntMatrix(indices);
        LongMatrix Q = P.multiplyRight(J);
        VerboseLongMatrix QT = Q.binary().transposed().longMatrix().verbose();
        // long seed = 0xf92165f6f21bfee9L;
        int[] rightLinear = { 0xebcb1c36, 0x59a5b1e1 };
        int[] leftLinear = { 0x2f06c4e5, 0xdc7ae9b5 };
        long expected = Integer.toUnsignedLong(leftLinear[0]) | (Integer.toUnsignedLong(leftLinear[1]) << 32);
        int[] y = new int[4];
        for (int k = 0; k < 2; k++) {
            for (int l = 0; l < 2; l++) {
                y[k * 2 + l] = (rightLinear[k] >>> (16 * l)) & 0xffff;
            }
        }
        long z = QT.multiplyLeft(y);
        assertEquals(expected, z);
    }
}
