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
        int s1PrimaryStart = uints[0] - s0PrimaryRange[0];
        int statePrimaryRangeWidth = s0PrimaryRange[1] - s0PrimaryRange[0];
        int[][] statePrimary = new int[statePrimaryRangeWidth][2];
        for (int i = 0; i < statePrimaryRangeWidth; i++) {
            int s0 = s0PrimaryRange[0] + i;
            int s1 = uints[0] - s0;
            statePrimary[i][0] = s0;
            statePrimary[i][1] = s1;
        }

        int[] baseRightLinear = RandomUIntSolver.linearRightUInts(0L, Xoroshiro.XOROSHIRO_CONST,
                Set.of(indicesArray[0]));
        LongMatrix I = RandomUIntSolver.linearRightUIntMatrix(Set.of(indicesArray[0]));
        int rank = I.enchelon().rank;
        LongMatrix J = I.generalizedInverse().longMatrix();
        LongMatrix H = I.multiplyRight(J).add(LongMatrix.ones());
        VerboseLongMatrix JT = J.binary().transposed().longMatrix().verbose();
        VerboseLongMatrix HT = H.binary().transposed().longMatrix().verbose();
        long[] nullspace = I.nullspace();

        int[] baseLeftLinear = RandomUIntSolver.linearLeftUInts(0L, Xoroshiro.XOROSHIRO_CONST, Set.of(indicesArray[0]));
        LongMatrix P = RandomUIntSolver.linearLeftUIntMatrix(Set.of(indicesArray[0]));
        VerboseLongMatrix PT = P.binary().transposed().longMatrix().verbose();

        int indexEndSecondaryExclusive = indicesArray[1] + 1;

        int[] found = new int[statePrimaryRangeWidth];
        MockMatrixKernel kernel = new MockMatrixKernel(nullspace, JT.leftMultiplied, rank, HT.leftMultiplied,
                PT.leftMultiplied, new int[] { uints[1] }, indicesArray[0], indexEndSecondaryExclusive, baseLeftLinear,
                baseRightLinear, s0PrimaryRange[0], s1PrimaryStart, found);

        int gid = s0s[0] - s0PrimaryRange[0];
        kernel.setGlobalId(gid);
        kernel.run();
        assertEquals(1, found[gid]);
    }

    public class MockMatrixKernel {

        final long[] nullspace;
        final long[][] J;
        final int rank;
        final long[][] H;
        final long[][] P;
        final int[] uintsSecondary;
        final int indexPrimary, indexEndSecondaryExclusive;
        final int[] baseLeftLinear, baseRightLinear;
        final int s0Start, s1Start;
        final int[] found;
        int gid;

        public MockMatrixKernel(long[] nullspace, long[][] j, int rank, long[][] h, long[][] p, int[] uintsSecondary,
                int indexPrimary, int indexEndSecondaryExclusive, int[] baseLeftLinear, int[] baseRightLinear,
                int s0Start, int s1Start, int[] found) {
            this.nullspace = nullspace;
            J = j;
            this.rank = rank;
            H = h;
            P = p;
            this.uintsSecondary = uintsSecondary;
            this.indexPrimary = indexPrimary;
            this.indexEndSecondaryExclusive = indexEndSecondaryExclusive;
            this.baseLeftLinear = baseLeftLinear;
            this.baseRightLinear = baseRightLinear;
            this.s0Start = s0Start;
            this.s1Start = s1Start;
            this.found = found;
        }

        void setGlobalId(int gid) {
            this.gid = gid;
        }

        private int getGlobalId() {
            return gid;
        }

        public void run() {
            int[] rightState = new int[2];
            int g = getGlobalId();
            rightState[0] = s0Start + g;
            rightState[1] = s1Start - g;
            int[] y = new int[2];
            for (int i = 0; i < 2; i++) {
                y[i] = baseRightLinear[i] ^ rightState[i];
            }
            int[] yShort = new int[2 * 2];
            for (int i = 0; i < 2; i++) {
                for (int k = 0; k < 2; k++) {
                    yShort[i * 2 + k] = (y[i] >>> (16 * k)) & 0xffff;
                }
            }
            if (rank < 64) {
                long syndrome = 0;
                for (int i = 0; i < 4; i++) {
                    syndrome ^= H[i][yShort[i]];
                }
                if (syndrome != 0) {
                    return;
                }
            }
            long x = 0;
            for (int i = 0; i < 4; i++) {
                x ^= J[i][yShort[i]];
            }
            for (int i = 0; i < nullspace.length; i++) {
                long seed = nullspace[i] ^ x;
                int[] seedShort = new int[4];
                for (int j = 0; j < 4; j++) {
                    seedShort[j] = (int) (seed >>> (j * 16)) & 0xffff;
                }
                long z = 0;
                for (int j = 0; j < 4; j++) {
                    z ^= P[j][seedShort[j]];
                }
                int[] leftState = new int[2];
                for (int j = 0; j < 2; j++) {
                    leftState[j] = baseLeftLinear[j] ^ (int) (z >>> (32 * j));
                }
                long s0 = (Integer.toUnsignedLong(leftState[0]) << 32) | Integer.toUnsignedLong(rightState[0]);
                long s1 = (Integer.toUnsignedLong(leftState[1]) << 32) | Integer.toUnsignedLong(rightState[1]);
                for (int j = indexPrimary; j < indexEndSecondaryExclusive; j++) {
                    int uint = (int) s0 + (int) s1;
                    if (binarySearch(uintsSecondary, 0, uintsSecondary.length, uint)) {
                        found[g] = 1;
                    }
                    s1 ^= s0;
                    s0 = rotl(s0, 24) ^ s1 ^ (s1 << 16);
                    s1 = rotl(s1, 37);
                }
            }
        }

        static long rotl(long x, int k) {
            return (x << k) | (x >>> (64 - k));
        }

        // java.util.Arrays
        // Like public version, but without range checks.
        private static boolean binarySearch(int[] a, int fromIndex, int toIndex, int key) {
            int low = fromIndex;
            int high = toIndex - 1;

            while (low <= high) {
                int mid = (low + high) >>> 1;
                int midVal = a[mid];

                if (midVal < key)
                    low = mid + 1;
                else if (midVal > key)
                    high = mid - 1;
                else
                    return true; // key found
            }
            return false; // key not found.
        }

    }

}