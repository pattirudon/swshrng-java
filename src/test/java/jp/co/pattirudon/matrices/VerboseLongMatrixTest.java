package jp.co.pattirudon.matrices;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;

import jp.co.pattirudon.RandomUIntSolver;

public class VerboseLongMatrixTest {

    @Test
    public void testMultiplyLeft() {
        Set<Integer> indices = Set.of(3, 4);
        LongMatrix I = RandomUIntSolver.linearUIntMatrix(indices);
        assertEquals(I.enchelon().rank, 64);
        BinaryMatrix J = I.generalizedInverse();
        VerboseLongMatrix JT = J.transposed().longMatrix().verbose();
        long seed = 0x9283ecd766451b9eL;
        int[] expected = { 0x4e65351f, 0xc27a1fd1, };
        int[] y = new int[4];
        for (int j = 0; j < 2; j++) {
            for (int k = 0; k < 2; k++) {
                y[j * 2 + k] = (expected[j] >>> (16 * k)) & 0xffff;
            }
        }
        long actual = JT.multiplyLeft(y);
        assertEquals(seed, actual);
    }
}
