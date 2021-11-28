package jp.co.pattirudon.swshrng;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Set;

import org.junit.Test;

import jp.co.pattirudon.swshrng.matrices.LongMatrix;
import jp.co.pattirudon.swshrng.matrices.VerboseLongMatrix;
import jp.co.pattirudon.swshrng.random.Xoroshiro;

public class MatrixKernelTest {

    @Test
    public void testMockMatrixKernel() {
        // int[] indices = new int[] { 2, 7 };
        // int[] uints = { 0x134c1f26, 0xdc01cbc4 };
        // int[] s0s = { 0x172e589b, 0xd8836c28 };
        // int[] s0PrimaryRange = { 0x172e5800, 0x172e5900 };
        int[] indices = new int[] { 15, 23 };
        int[] uints = { 0x42f20304, 0x9cd94855 };
        int s0Primary = 0xc76b6b2e;
        int[] s0PrimaryRange = { 0xc76b6b2e, 0xc76b6b2f };
        int s0StartPrimary = s0PrimaryRange[0];
        int s1StartPrimary = uints[0] - s0StartPrimary;
        int statePrimaryRangeWidth = s0PrimaryRange[1] - s0PrimaryRange[0];

        int[] baseRightState = RandomUIntSolver.linearRightUInts(0L, Xoroshiro.XOROSHIRO_CONST, Set.of(indices[0]));
        LongMatrix I = RandomUIntSolver.linearRightUIntMatrix(Set.of(indices[0]));
        LongMatrix J = I.generalizedInverse().longMatrix();
        LongMatrix H = I.multiplyRight(J).add(LongMatrix.ones());
        // VerboseLongMatrix JT = J.binary().transposed().longMatrix().verbose();
        VerboseLongMatrix HT = H.binary().transposed().longMatrix().verbose();
        long[] nullspace = I.nullspace();
        long[][] nullimage = new long[nullspace.length][2];
        for (int k = 0; k < nullspace.length; k++) {
            nullimage[k] = RandomUIntSolver.state(nullspace[k], Xoroshiro.XOROSHIRO_CONST, Set.of(indices[1]));
        }

        long[] baseState = RandomUIntSolver.state(0L, Xoroshiro.XOROSHIRO_CONST, Set.of(indices[1]));
        VerboseLongMatrix[] Sq = new VerboseLongMatrix[2];
        LongMatrix S = RandomUIntSolver.stateMatrix(Set.of(indices[1])).multiplyRight(J);
        for (int k = 0; k < 2; k++) {
            LongMatrix halvedS = LongMatrix.getInstance(Arrays.copyOfRange(S.mat, k * 64, (k + 1) * 64), false);
            VerboseLongMatrix halvedST = halvedS.binary().transposed().longMatrix().verbose();
            Sq[k] = halvedST;
        }

        int[] found = new int[statePrimaryRangeWidth];
        MockMatrixKernel kernel = new MockMatrixKernel(nullimage, HT.leftMultiplied,
                new long[][][] { Sq[0].leftMultiplied, Sq[1].leftMultiplied }, new int[] { uints[1] }, indices[1],
                indices[1] + 1, baseRightState, baseState, s0StartPrimary, s1StartPrimary, found);

        int gid = s0Primary - s0PrimaryRange[0];
        kernel.setGlobalId(gid);
        kernel.run();
        assertEquals(1, found[gid]);
    }

    public class MockMatrixKernel {

        int gid;

        void setGlobalId(int gid) {
            this.gid = gid;
        }

        private int getGlobalId() {
            return gid;
        }

        final long[][] nullimage;
        final long[][] H;
        final long[][][] S;
        final int[] uintsSecondary;
        final int indexStartSecondaryInclusive, indexEndSecondaryExclusive;
        final int[] baseRightStatePrimary;
        final long[] baseStateSecondary;
        final int s0Start, s1Start;
        final int[] found;

        public MockMatrixKernel(long[][] nullimage, long[][] h, long[][][] s, int[] uintsSecondary,
                int indexStartSecondaryInclusive, int indexEndSecondaryExclusive, int[] baseRightStatePrimary,
                long[] baseStateSecondary, int s0Start, int s1Start, int[] found) {
            this.nullimage = nullimage;
            this.H = h;
            this.S = s;
            this.uintsSecondary = uintsSecondary;
            this.indexStartSecondaryInclusive = indexStartSecondaryInclusive;
            this.indexEndSecondaryExclusive = indexEndSecondaryExclusive;
            this.baseRightStatePrimary = baseRightStatePrimary;
            this.baseStateSecondary = baseStateSecondary;
            this.s0Start = s0Start;
            this.s1Start = s1Start;
            this.found = found;
        }

        public void run() {
            int[] rightState = new int[2];
            int g = getGlobalId();
            rightState[0] = s0Start + g;
            rightState[1] = s1Start - g;
            int[] y = new int[2];
            for (int i = 0; i < 2; i++) {
                y[i] = baseRightStatePrimary[i] ^ rightState[i];
            }
            int[] yShort = new int[2 * 2];
            for (int i = 0; i < 2; i++) {
                for (int k = 0; k < 2; k++) {
                    yShort[i * 2 + k] = (y[i] >>> (16 * k)) & 0xffff;
                }
            }
            if (nullimage.length > 1) {
                long syndrome = 0;
                for (int i = 0; i < 4; i++) {
                    syndrome ^= H[i][yShort[i]];
                }
                if (syndrome != 0) {
                    return;
                }
            }
            long[] z = new long[2];
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 4; j++) {
                    z[i] ^= S[i][j][yShort[j]];
                }
            }
            for (int i = 0; i < nullimage.length; i++) {
                long s0 = z[0] ^ nullimage[i][0];
                long s1 = z[1] ^ nullimage[i][1];
                for (int j = indexStartSecondaryInclusive; j < indexEndSecondaryExclusive; j++) {
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