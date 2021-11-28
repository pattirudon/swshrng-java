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

import jp.co.pattirudon.config.RandomUIntSolverConfig;
import jp.co.pattirudon.matrices.LongMatrix;
import jp.co.pattirudon.matrices.VerboseLongMatrix;
import jp.co.pattirudon.pokemon.OverworldPokemon;
import jp.co.pattirudon.random.Xoroshiro;
import jp.co.pattirudon.random.XoroshiroAdapter;

public class RandomUIntSolver {

    final int indexStartSecondaryInclusive, indexEndSecondaryExclusive;
    final long[][] J, H;
    final long[][][] S;
    final long[] nullspace;
    final long[][] nullimage;
    final int[] baseRightState;
    // final long[] baseState;

    public RandomUIntSolver(int indexPrimary, int indexStartSecondaryInclusive, int indexEndSecondaryExclusive) {
        this.indexStartSecondaryInclusive = indexStartSecondaryInclusive;
        this.indexEndSecondaryExclusive = indexEndSecondaryExclusive;

        int[] baseRightState = RandomUIntSolver.linearRightUInts(0L, Xoroshiro.XOROSHIRO_CONST, Set.of(indexPrimary));
        LongMatrix I = RandomUIntSolver.linearRightUIntMatrix(Set.of(indexPrimary));
        LongMatrix J = I.generalizedInverse().longMatrix();
        LongMatrix H = I.multiplyRight(J).add(LongMatrix.ones());
        VerboseLongMatrix JT = J.binary().transposed().longMatrix().verbose();
        VerboseLongMatrix HT = H.binary().transposed().longMatrix().verbose();
        long[] nullspace = I.nullspace();
        this.nullspace = nullspace;
        this.nullimage = new long[nullspace.length][2];
        for (int k = 0; k < nullspace.length; k++) {
            this.nullimage[k] = state(nullspace[k], Xoroshiro.XOROSHIRO_CONST, Set.of(indexStartSecondaryInclusive));
        }
        this.baseRightState = baseRightState;
        this.J = JT.leftMultiplied;
        this.H = HT.leftMultiplied;

        // long[] baseState = RandomUIntSolver.state(0L, Xoroshiro.XOROSHIRO_CONST, Set.of(indexStartSecondaryInclusive));
        // this.baseState = baseState;
        this.S = new long[2][][];
        LongMatrix S = RandomUIntSolver.stateMatrix(Set.of(indexStartSecondaryInclusive)).multiplyRight(J);
        for (int k = 0; k < 2; k++) {
            LongMatrix halvedS = LongMatrix.getInstance(Arrays.copyOfRange(S.mat, k * 64, (k + 1) * 64), false);
            VerboseLongMatrix halvedST = halvedS.binary().transposed().longMatrix().verbose();
            this.S[k] = halvedST.leftMultiplied;
        }
    }

    public LongStream solve(int uintPrimary, int[] uintsSecondary, int rightS0StartInclusive, int rightS0EndExclusive) {

        int statePrimaryRangeWidth = rightS0EndExclusive - rightS0StartInclusive;

        int s0Start = rightS0StartInclusive;
        int s1Start = uintPrimary - s0Start;

        int[] found = new int[statePrimaryRangeWidth];
        Kernel kernel = new MatrixKernel(nullimage, H, S, uintsSecondary, indexStartSecondaryInclusive,
                indexEndSecondaryExclusive, baseRightState, s0Start, s1Start, found);
        kernel.execute(Range.create(statePrimaryRangeWidth));
        kernel.dispose();

        IntStream foundGids = IntStream.range(0, statePrimaryRangeWidth).parallel().filter(new IntPredicate() {
            @Override
            public boolean test(int gid) {
                return found[gid] == 1;
            }
        });
        LongStream foundBaseSeeds = foundGids.mapToLong(new IntToLongFunction() {
            @Override
            public long applyAsLong(int g) {
                int[] rightState = new int[2];
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
                long x = 0;
                for (int i = 0; i < yShort.length; i++) {
                    x ^= J[i][yShort[i]];
                }
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
                for (int l = 0; l < indexStartSecondaryInclusive; l++) {
                    random.nextInt();
                }
                for (int l = indexStartSecondaryInclusive; l < indexEndSecondaryExclusive; l++) {
                    int uint = random.nextInt();
                    if (Arrays.binarySearch(uintsSecondary, uint) >= 0) {
                        match = true;
                        break;
                    }
                }
                return match;
            };
        });
        return foundSeeds.sequential();
    }

    public LongStream solve(int uintPrimary, int[] uintsSecondary) {
        int statePrimaryRangeWidth = 0x100_0000;
        int statePrimaryRangeCount = 0x100;
        LongStream ls = LongStream.range(0, statePrimaryRangeCount).flatMap(new LongFunction<LongStream>() {
            @Override
            public LongStream apply(long rangeIndex) {
                int s0StartInclusive = statePrimaryRangeWidth * (int) rangeIndex;
                int s0EndExclusive = s0StartInclusive + statePrimaryRangeWidth;
                return solve(uintPrimary, uintsSecondary, s0StartInclusive, s0EndExclusive);
            }
        });
        return ls;
    }

    public static long[] state(long seed0, long seed1, Set<Integer> indices) {
        Xoroshiro random = new Xoroshiro(seed0, seed1);
        Set<Integer> _indices = new TreeSet<>(indices);
        long[] linear = new long[_indices.size() * 2];
        for (int i = 0, j = 0; !_indices.isEmpty(); i++) {
            if (_indices.contains(i)) {
                for (int k = 0; k < 2; k++) {
                    long u = random.s[k];
                    linear[j * 2 + k] = u;
                }
                j++;
                _indices.remove(i);
            }
            random.next();
        }
        return linear;
    }

    public static LongMatrix stateMatrix(Set<Integer> indices) {
        LongFunction<long[]> l = seed -> state(seed, 0L, indices);
        return LongMatrix.representLongLinear(l, indices.size() * 2);
    }

    public static int[] linearRightUInts(long seed0, long seed1, Set<Integer> indices) {
        long[] l = state(seed0, seed1, indices);
        int[] k = new int[l.length];
        for (int i = 0; i < l.length; i++) {
            k[i] = (int) l[i] & 0xffffffff;
        }
        return k;
    }

    public static LongMatrix linearRightUIntMatrix(Set<Integer> indices) {
        LongFunction<int[]> l = seed -> linearRightUInts(seed, 0L, indices);
        return LongMatrix.representPairwiseIntLinear(l, indices.size() * 2);
    }

    public static int[] linearLeftUInts(long seed0, long seed1, Set<Integer> indices) {
        long[] l = state(seed0, seed1, indices);
        int[] k = new int[l.length];
        for (int i = 0; i < l.length; i++) {
            k[i] = (int) (l[i] >>> 32);
        }
        return k;
    }

    public static LongMatrix linearLeftUIntMatrix(Set<Integer> indices) {
        LongFunction<int[]> l = seed -> linearLeftUInts(seed, 0L, indices);
        return LongMatrix.representPairwiseIntLinear(l, indices.size() * 2);
    }

    public static void list(RandomUIntSolverConfig config, Logger logger) {
        int[] indicesPrimary = IntStream.range(config.primary.frame.startInclusive, config.primary.frame.endExclusive)
                .toArray();
        int indexStartSecondaryInclusive = config.secondary.frame.startInclusive;
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
                    logger.info(String.format("frame=%d, localseed=%08x", indexPrimary, uintPrimary));
                    RandomUIntSolver solver = new RandomUIntSolver(indexPrimary, indexStartSecondaryInclusive,
                            indexEndSecondaryExclusive);
                    LongStream ls = solver.solve(uintPrimary, uintsSecondary);
                    ls.forEach(new LongConsumer() {
                        @Override
                        public void accept(long seed) {
                            int indexEnd = Math.max(indexPrimary, indexEndSecondaryExclusive);
                            XoroshiroAdapter random = new XoroshiroAdapter(seed, config.tsv, config.hasShinyCharm,
                                    config.hasMarkCharm, config.isWeatherActive, config.isFishing);
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
