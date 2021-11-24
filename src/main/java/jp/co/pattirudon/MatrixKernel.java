package jp.co.pattirudon;

import com.aparapi.Kernel;

import jp.co.pattirudon.random.Xoroshiro;

public class MatrixKernel extends Kernel {

    final long[] nullspace;
    final long[][] J;
    final long[][] H;
    final int[] uintsSecondary;
    final int indexEndSecondaryExclusive;
    final int[] baseLinear;
    final int s0Start, s1Start;
    final int[] found;

    public MatrixKernel(long[] nullspace, long[][] j, long[][] h, int[] uintsSecondary, int indexEndSecondaryExclusive,
            int[] baseLinear, int s0Start, int s1Start, int[] found) {
        this.nullspace = nullspace;
        J = j;
        H = h;
        this.uintsSecondary = uintsSecondary;
        this.indexEndSecondaryExclusive = indexEndSecondaryExclusive;
        this.baseLinear = baseLinear;
        this.s0Start = s0Start;
        this.s1Start = s1Start;
        this.found = found;
    }

    @Override
    public void run() {
        int[] stap = new int[2];
        int g = getGlobalId();
        stap[0] = s0Start + g;
        stap[1] = s1Start - g;
        int[] y = new int[2];
        for (int i = 0; i < 2; i++) {
            y[i] = baseLinear[i] ^ stap[i];
        }
        int[] yShort = new int[2 * 2];
        for (int i = 0; i < 2; i++) {
            for (int k = 0; k < 2; k++) {
                yShort[i * 2 + k] = (y[i] >>> (16 * k)) & 0xffff;
            }
        }
        long syndrome = 0;
        for (int i = 0; i < 4; i++) {
            syndrome ^= H[i][yShort[i]];
        }
        if (syndrome != 0) {
            return;
        }
        long x = 0;
        for (int i = 0; i < 4; i++) {
            x ^= J[i][yShort[i]];
        }
        for (int i = 0; i < nullspace.length; i++) {
            long seed = nullspace[i] ^ x;
            long s0 = seed;
            long s1 = Xoroshiro.XOROSHIRO_CONST;
            for (int j = 0; j < indexEndSecondaryExclusive; j++) {
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
