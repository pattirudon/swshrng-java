package jp.co.pattirudon;

import com.aparapi.Kernel;

public class MatrixKernel extends Kernel {

    final long[] nullspace;
    final long[][] J;
    final int rank;
    final long[][] H;
    final long[][] P;
    final int[] uintsSecondary;
    final int indexPrimary, indexStartSecondaryInclusive, indexEndSecondaryExclusive;
    final int[] baseLeftState, baseRightState;
    final int s0Start, s1Start;
    final int[] found;

    public MatrixKernel(long[] nullspace, long[][] j, int rank, long[][] h, long[][] p, int[] uintsSecondary,
            int indexPrimary, int indexStartSecondaryInclusive, int indexEndSecondaryExclusive, int[] baseLeftState,
            int[] baseRightState, int s0Start, int s1Start, int[] found) {
        this.nullspace = nullspace;
        J = j;
        this.rank = rank;
        H = h;
        P = p;
        this.uintsSecondary = uintsSecondary;
        this.indexPrimary = indexPrimary;
        this.indexStartSecondaryInclusive = indexStartSecondaryInclusive;
        this.indexEndSecondaryExclusive = indexEndSecondaryExclusive;
        this.baseLeftState = baseLeftState;
        this.baseRightState = baseRightState;
        this.s0Start = s0Start;
        this.s1Start = s1Start;
        this.found = found;
    }

    @Override
    public void run() {
        int[] rightState = new int[2];
        int g = getGlobalId();
        rightState[0] = s0Start + g;
        rightState[1] = s1Start - g;
        int[] y = new int[2];
        for (int i = 0; i < 2; i++) {
            y[i] = baseRightState[i] ^ rightState[i];
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
                leftState[j] = baseLeftState[j] ^ (int) (z >>> (32 * j));
            }
            // long s0 = (Integer.toUnsignedLong(leftState[0]) << 32) | Integer.toUnsignedLong(rightState[0]);
            // long s1 = (Integer.toUnsignedLong(leftState[1]) << 32) | Integer.toUnsignedLong(rightState[1]);
            long s0 = ((leftState[0] & 0xffffffffL) << 32) | (rightState[0] & 0xffffffffL);
            long s1 = ((leftState[1] & 0xffffffffL) << 32) | (rightState[1] & 0xffffffffL);
            for (int j = indexPrimary; j < indexStartSecondaryInclusive; j++) {
                s1 ^= s0;
                s0 = rotl(s0, 24) ^ s1 ^ (s1 << 16);
                s1 = rotl(s1, 37);
            }
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
