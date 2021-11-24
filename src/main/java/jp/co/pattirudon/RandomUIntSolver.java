package jp.co.pattirudon;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntPredicate;
import java.util.function.IntToLongFunction;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import com.aparapi.Kernel;
import com.aparapi.Range;

import jp.co.pattirudon.matrices.BinaryMatrix;
import jp.co.pattirudon.matrices.LongMatrix;
import jp.co.pattirudon.matrices.VerboseLongMatrix;
import jp.co.pattirudon.pokemon.OverworldPokemon;
import jp.co.pattirudon.random.Xoroshiro;
import jp.co.pattirudon.random.XoroshiroAdapter;

public class RandomUIntSolver {

    public static int[] linearUInts(long seed0, long seed1, Set<Integer> indices) {
        Xoroshiro random = new Xoroshiro(seed0, seed1);
        Set<Integer> _indices = new TreeSet<>(indices);
        int[] linear = new int[_indices.size() * 2];
        for (int i = 0, j = 0; !_indices.isEmpty(); i++) {
            if (_indices.contains(i)) {
                for (int k = 0; k < 2; k++) {
                    int uint = (int) random.s[k];
                    linear[j * 2 + k] = uint;
                }
                j++;
                _indices.remove(i);
            }
            random.next();
        }
        return linear;
    }

    public static LongMatrix linearUIntMatrix(Set<Integer> indices) {
        byte[][] t = new byte[64][32 * indices.size() * 2];
        for (int i = 0; i < 64; i++) {
            long seed = 1L << i;
            int[] linear = linearUInts(seed, 0L, indices);
            for (int j = 0; j < linear.length; j++) {
                for (int k = 0; k < 32; k++) {
                    t[i][j * 32 + k] = (byte) ((linear[j] >>> k) & 1);
                }
            }
        }
        return BinaryMatrix.getInstance(64, 32 * indices.size() * 2, t, false).transposed().longMatrix();
    }

    public static LongStream solve(int indexPrimary, int indexEndSecondaryExclusive, int uintPrimary,
            int[] uintsSecondary) {
        int statePrimaryRangeWidth = 0x100_0000;
        int statePrimaryRangeCount = 0x100;
        LongStream ls = LongStream.range(0, statePrimaryRangeCount).flatMap(new LongFunction<LongStream>() {
            @Override
            public LongStream apply(long rangeIndex) {
                int s0StartInclusive = statePrimaryRangeWidth * (int) rangeIndex;
                int s0EndExclusive = s0StartInclusive + statePrimaryRangeWidth;
                return solve(indexPrimary, indexEndSecondaryExclusive, uintPrimary, uintsSecondary, s0StartInclusive,
                        s0EndExclusive);
            }
        });
        return ls;
    }

    public static LongStream solve(int indexPrimary, int indexEndSecondaryExclusive, int uintPrimary,
            int[] uintsSecondary, int s0StartInclusive, int s0EndExclusive) {
        int[] baseLinear = RandomUIntSolver.linearUInts(0L, Xoroshiro.XOROSHIRO_CONST, Set.of(indexPrimary));
        LongMatrix I = RandomUIntSolver.linearUIntMatrix(Set.of(indexPrimary));
        LongMatrix J = I.generalizedInverse().longMatrix();
        LongMatrix H = I.multiplyRight(J).add(LongMatrix.ones());
        VerboseLongMatrix JT = J.binary().transposed().longMatrix().verbose();
        VerboseLongMatrix HT = H.binary().transposed().longMatrix().verbose();
        long[] nullspace = I.nullspace();

        int statePrimaryRangeWidth = s0EndExclusive - s0StartInclusive;

        int s0Start = s0StartInclusive;
        int s1Start = uintPrimary - s0Start;

        int[] found = new int[statePrimaryRangeWidth];
        Kernel kernel = new MatrixKernel(nullspace, JT.leftMultiplied, HT.leftMultiplied, uintsSecondary,
                indexEndSecondaryExclusive, baseLinear, s0Start, s1Start, found);
        kernel.execute(Range.create(statePrimaryRangeWidth));
        kernel.dispose();

        IntStream foundGids = IntStream.range(0, statePrimaryRangeWidth).parallel().filter(new IntPredicate() {
            @Override
            public boolean test(int gid) {
                return found[gid] == 1;
            }
        }).sequential();
        LongStream foundBaseSeeds = foundGids.mapToLong(new IntToLongFunction() {
            @Override
            public long applyAsLong(int g) {
                int[] stap = new int[2];
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
                long x = JT.multiplyLeft(yShort);
                return x;
            }
        });
        LongStream foundSeeds = foundBaseSeeds.flatMap(new LongFunction<LongStream>() {
            @Override
            public LongStream apply(long x) {
                return Arrays.stream(nullspace).map(k -> x ^ k);
            };
        }).filter(new LongPredicate() {
            @Override
            public boolean test(long seed) {
                Xoroshiro random = new Xoroshiro(seed);
                boolean match = false;
                for (int l = 0; l < indexEndSecondaryExclusive; l++) {
                    int uint = random.nextInt();
                    if (Arrays.binarySearch(uintsSecondary, uint) >= 0) {
                        match = true;
                        break;
                    }
                }
                return match;
            };
        });
        return foundSeeds;
    }

    public static void list(RandomUIntSolverConfig config, Logger logger) {
        int[] indicesPrimary = IntStream.range(config.primary.frame.startInclusive, config.primary.frame.endExclusive)
                .toArray();
        int indexEndSecondaryExclusive = config.secondary.frame.endExclusive;

        int[] ivsPrimary = { config.primary.ivs.h, config.primary.ivs.a, config.primary.ivs.b, config.primary.ivs.c,
                config.primary.ivs.d, config.primary.ivs.s };
        List<Integer> uintsPrimaryList = RandomIVSolver.solve(ivsPrimary);
        int[] uintsPrimary = uintsPrimaryList.stream().mapToInt(Integer::intValue).toArray();

        int[] ivsSecondary = { config.secondary.ivs.h, config.secondary.ivs.a, config.secondary.ivs.b,
                config.secondary.ivs.c, config.secondary.ivs.d, config.secondary.ivs.s };
        List<Integer> uintsSecondaryList = RandomIVSolver.solve(ivsSecondary);
        int[] uintsSecondary = uintsSecondaryList.stream().mapToInt(Integer::intValue).toArray();

        if (uintsPrimary != null && uintsPrimary.length > 0 && uintsSecondary != null && uintsSecondary.length > 0) {
            for (int h = 0; h < indicesPrimary.length; h++) {
                int indexPrimary = indicesPrimary[h];
                for (int i = 0; i < uintsPrimary.length; i++) {
                    int uintPrimary = uintsPrimary[i];
                    LongStream ls = solve(indexPrimary, indexEndSecondaryExclusive, uintPrimary, uintsSecondary);
                    ls.forEach(new LongConsumer() {
                        @Override
                        public void accept(long seed) {
                            int indexEnd = Math.max(indexPrimary, indexEndSecondaryExclusive);
                            XoroshiroAdapter random = new XoroshiroAdapter(seed);
                            for (int i = 0; i < indexEnd; i++) {
                                OverworldPokemon p = random.sampleOverworld();
                                if ((Arrays.binarySearch(uintsPrimary, p.seedLocal) >= 0
                                        && p.nature == config.primary.nature)
                                        || Arrays.binarySearch(uintsSecondary, p.seedLocal) >= 0
                                                && p.nature == config.secondary.nature) {
                                    logger.info(String.format("(%016x, %d): %s", seed, i, p));
                                }
                            }
                        }
                    });
                }
            }
        }
    }

}
