package jp.co.pattirudon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import jp.co.pattirudon.matrices.BinaryMatrix;
import jp.co.pattirudon.matrices.IntMatrix;
import jp.co.pattirudon.matrices.LongMatrix;
import jp.co.pattirudon.matrices.VerboseIntMatrix;
import jp.co.pattirudon.random.Xoroshiro;
import jp.co.pattirudon.util.IterTools;

public class RandomIVSolver {
    public static Set<Integer> possibleXors(int n, int summation) {
        Set<Integer> xors = new TreeSet<>();
        int mask = (1 << n) - 1;
        for (int a = 0; a < (1 << n); a++) {
            int b = (summation - a) & mask;
            xors.add(a ^ b);
        }
        return xors;
    }

    public static Set<Integer> possibleXorsWithoutCarry(int n, int summation) {
        Set<Integer> xors = new TreeSet<>();
        for (int a = 0, b = summation; a <= b; a++, b--) {
            xors.add(a ^ b);
        }
        return xors;
    }

    public static Set<Integer> possibleXorsWithCarry(int n, int summation) {
        Set<Integer> xors = new TreeSet<>();
        for (int a = summation + 1, b = (1 << n) - 1; a <= b; a++, b--) {
            xors.add(a ^ b);
        }
        return xors;
    }

    public static Set<Integer> possibleXors(int summation) {
        int left_sum = summation >>> 16;
        int right_sum = summation & 0xffff;
        Set<Integer> a = possibleXors(16, left_sum);
        Set<Integer> b = possibleXors(16, (left_sum - 1) & 0xffff);
        Set<Integer> c = possibleXorsWithoutCarry(16, right_sum);
        Set<Integer> d = possibleXorsWithCarry(16, right_sum);
        Set<Integer> xors = new TreeSet<>();
        for (int x : a) {
            for (int y : c) {
                xors.add((x << 16) | y);
            }
        }
        for (int x : b) {
            for (int y : d) {
                xors.add((x << 16) | y);
            }
        }
        return xors;
    }

    public static int[] linearIVs(long seed0, long seed1) {
        Xoroshiro random = new Xoroshiro(seed0, seed1);
        random.nextInt();
        random.nextInt();
        int[] ivs = new int[6];
        for (int i = 0; i < ivs.length; i++) {
            int iv_omote = (int) random.s[0] & 0x1f;
            int iv_ura = (int) random.s[1] & 0x1f;
            ivs[i] = iv_omote ^ iv_ura;
            random.nextInt();
        }
        return ivs;
    }

    public static IntMatrix linearIVMatrix() {
        byte[][] t = new byte[32][30];
        for (int i = 0; i < 32; i++) {
            int seed = 1 << i;
            int[] linear = linearIVs(Integer.toUnsignedLong(seed), 0);
            for (int j = 0; j < 6; j++) {
                for (int k = 0; k < 5; k++) {
                    t[i][j * 5 + k] = (byte) ((linear[j] >>> k) & 1);
                }
            }
        }
        return BinaryMatrix.getInstance(32, 30, t, false).transposed().intMatrix();
    }

    public static List<Integer> solve(int[] ivs) {
        if (ivs.length != 6) {
            throw new IllegalArgumentException("The length of array must be 6.");
        }
        IntMatrix I = linearIVMatrix();
        BinaryMatrix J = I.generalizedInverse();
        VerboseIntMatrix JT = J.transposed().intMatrix().verbose();
        int[] nullspace = I.nullspace();

        int[] baseLinear = linearIVs(0L, Xoroshiro.XOROSHIRO_CONST);

        List<Set<? extends Integer>> xorList = Arrays.stream(ivs)
                .<Set<? extends Integer>>mapToObj(iv -> possibleXors(5, iv)).toList();

        List<Integer> result = new ArrayList<>();

        for (List<Integer> xors : IterTools.<Integer>cartesianProduct(xorList)) {
            int[] y = new int[ivs.length];
            Arrays.setAll(y, k -> baseLinear[k] ^ xors.get(k));
            int x = JT.multiplyLeft(y);
            for (int l = 0; l < nullspace.length; l++) {
                int seed = x ^ nullspace[l];
                Xoroshiro random = new Xoroshiro(Integer.toUnsignedLong(seed));
                random.nextInt();
                random.nextInt();
                int[] _ivs = new int[ivs.length];
                Arrays.setAll(_ivs, k -> random.nextInt() & 0x1f);
                if (Arrays.equals(_ivs, ivs)) {
                    result.add(seed);
                }
            }
        }
        return result;
    }

    public static int[] _linearUInts(long seed0, long seed1, Set<Integer> indices) {
        Xoroshiro random = new Xoroshiro(seed0, seed1);
        Set<Integer> _indices = new TreeSet<>(indices);
        int[] linear = new int[_indices.size()];
        for (int i = 0, j = 0; !_indices.isEmpty(); i++) {
            if (_indices.contains(i)) {
                int uint = (int) random.s[0] ^ (int) random.s[1];
                linear[j] = uint;
                j++;
                _indices.remove(i);
            }
            random.next();
        }
        return linear;
    }

    public static LongMatrix _linearUIntMatrix(Set<Integer> indices) {
        byte[][] t = new byte[64][32 * indices.size()];
        for (int i = 0; i < 64; i++) {
            long seed = 1L << i;
            int[] linear = _linearUInts(seed, 0L, indices);
            for (int j = 0; j < linear.length; j++) {
                for (int k = 0; k < 32; k++) {
                    t[i][j * 32 + k] = (byte) ((linear[j] >>> k) & 1);
                }
            }
        }
        return BinaryMatrix.getInstance(64, 32 * indices.size(), t, false).transposed().longMatrix();
    }

    public static void list(RandomIVSolverConfig config, Logger logger) {
        int[] ivs = { config.ivs.h, config.ivs.a, config.ivs.b, config.ivs.c, config.ivs.d, config.ivs.s };
        List<Integer> seeds = solve(ivs);
        for (int i = 0; i < seeds.size(); i++) {
            int seed = seeds.get(i);
            logger.info(String.format("%08x", seed));
        }
        logger.config(String.format("%d seed(s) were found.", seeds.size()));
    }

}
