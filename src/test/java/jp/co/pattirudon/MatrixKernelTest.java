package jp.co.pattirudon;

import static org.junit.Assert.assertEquals;

import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import jp.co.pattirudon.matrices.LongMatrix;
import jp.co.pattirudon.matrices.VerboseLongMatrix;
import jp.co.pattirudon.random.Xoroshiro;

public class MatrixKernelTest {

    @Test
    public void testMockMatrixKernel() {
        int[] indicesArray = new int[] { 2, 7 };
        Set<Integer> indices = new TreeSet<>();
        for (int i = 0; i < indicesArray.length; i++) {
            indices.add(indicesArray[i]);
        }
        int[] uints = { 0x134c1f26, 0xdc01cbc4 };
        // int[] uintsLinear = { 0xeb339e10, 0xdbfd33b4 };
        int[] s0s = { 0x172e589b, 0xd8836c28 };

        int[] s0PrimaryRange = { 0x172e5800, 0x172e5900 };
        int statePrimaryRangeWidth = s0PrimaryRange[1] - s0PrimaryRange[0];
        int[][] statePrimary = new int[statePrimaryRangeWidth][2];
        for (int i = 0; i < statePrimaryRangeWidth; i++) {
            int s0 = s0PrimaryRange[0] + i;
            int s1 = uints[0] - s0;
            statePrimary[i][0] = s0;
            statePrimary[i][1] = s1;
        }

        int[] baseLinear = RandomUIntSolver.linearUInts(0L, Xoroshiro.XOROSHIRO_CONST, Set.of(indicesArray[0]));
        LongMatrix I = RandomUIntSolver.linearUIntMatrix(Set.of(indicesArray[0]));
        LongMatrix J = I.generalizedInverse().longMatrix();
        LongMatrix H = I.multiplyRight(J).add(LongMatrix.ones());
        VerboseLongMatrix JT = J.binary().transposed().longMatrix().verbose();
        VerboseLongMatrix HT = H.binary().transposed().longMatrix().verbose();
        long[] nullspace = I.nullspace();

        long[] result = new long[statePrimaryRangeWidth];
        MockMatrixKernel kernel = new MockMatrixKernel(nullspace, JT.leftMultiplied, HT.leftMultiplied, uints[1],
                indicesArray[1], statePrimary, baseLinear, result);
        int gid = s0s[0] - s0PrimaryRange[0];
        kernel.setGlobalId(gid);
        kernel.run();
        assertEquals(0xd3631b20643deba2L, kernel.result[gid]);
    }

    public class MockMatrixKernel {

        final long[] nullspace;
        final long[][] J;
        final long[][] H;
        final int uintSecondary;
        final int indexSecondary;
        final int[][] statePrimary;
        final int[] baseLinear;
        final long[] result;
        int globalId;

        public MockMatrixKernel(long[] nullspace, long[][] j, long[][] h, int uintSecondary, int indexSecondary,
                int[][] statePrimary, int[] baseLinear, long[] result) {
            this.nullspace = nullspace;
            J = j;
            H = h;
            this.uintSecondary = uintSecondary;
            this.indexSecondary = indexSecondary;
            this.statePrimary = statePrimary;
            this.baseLinear = baseLinear;
            this.result = result;
        }

        private int getGlobalId() {
            return globalId;
        }

        void setGlobalId(int i) {
            this.globalId = i;
        }

        public void run() {
            int[] stap = new int[2];
            for (int i = 0; i < stap.length; i++) {
                int g = getGlobalId();
                stap[i] = statePrimary[g][i];
            }
            int[] y = new int[2];
            for (int i = 0; i < y.length; i++) {
                y[i] = baseLinear[i] ^ stap[i];
            }
            int[] yShort = new int[2 * 2];
            for (int i = 0; i < y.length; i++) {
                for (int k = 0; k < 2; k++) {
                    yShort[i * 2 + k] = (y[i] >>> (16 * k)) & 0xffff;
                }
            }
            long syndrome = 0;
            for (int i = 0; i < yShort.length; i++) {
                syndrome ^= H[i][yShort[i]];
            }
            if (syndrome != 0) {
                return;
            }
            long x = 0;
            for (int i = 0; i < yShort.length; i++) {
                x ^= J[i][yShort[i]];
            }
            for (int i = 0; i < nullspace.length; i++) {
                long seed = nullspace[i] ^ x;
                long s0 = seed;
                long s1 = Xoroshiro.XOROSHIRO_CONST;
                for (int j = 0; j < indexSecondary; j++) {
                    s1 ^= s0;
                    s0 = rotl(s0, 24) ^ s1 ^ (s1 << 16);
                    s1 = rotl(s1, 37);
                }
                int uint = (int) s0 + (int) s1;
                if (uint == uintSecondary) {
                    result[getGlobalId()] = seed;
                }
            }
        }

        static long rotl(long x, int k) {
            return (x << k) | (x >>> (64 - k));
        }

    }

}