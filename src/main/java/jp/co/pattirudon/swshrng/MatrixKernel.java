package jp.co.pattirudon.swshrng;

import com.aparapi.Kernel;

public class MatrixKernel extends Kernel {

    final long[][] nullimage;
    final long[][] H;
    final long[][] S0, S1;
    final int[] uintsSecondary;
    final int indexStartSecondaryInclusive, indexEndSecondaryExclusive;
    final int[] baseRightStatePrimary;
    final int s0Start, s1Start;
    final int[] found;

    public MatrixKernel(long[][] nullimage, long[][] h, long[][][] s, int[] uintsSecondary,
            int indexStartSecondaryInclusive, int indexEndSecondaryExclusive, int[] baseRightStatePrimary, int s0Start,
            int s1Start, int[] found) {
        this.nullimage = nullimage;
        this.H = h;
        this.S0 = s[0];
        this.S1 = s[1];
        this.uintsSecondary = uintsSecondary;
        this.indexStartSecondaryInclusive = indexStartSecondaryInclusive;
        this.indexEndSecondaryExclusive = indexEndSecondaryExclusive;
        this.baseRightStatePrimary = baseRightStatePrimary;
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
        long z0 = 0L;
        for (int j = 0; j < 4; j++) {
            z0 ^= S0[j][yShort[j]];
        }
        long z1 = 0L;
        for (int j = 0; j < 4; j++) {
            z1 ^= S1[j][yShort[j]];
        }
        for (int i = 0; i < nullimage.length; i++) {
            long s0 = z0 ^ nullimage[i][0];
            long s1 = z1 ^ nullimage[i][1];
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
