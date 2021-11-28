package jp.co.pattirudon.swshrng.matrices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.LongFunction;

public class LongMatrix {
    public final int rows;
    public final long[] mat;
    public final int columns = 64;

    protected LongMatrix(long[] mat) {
        this.rows = mat.length;
        this.mat = mat;
    }

    public static LongMatrix getInstance(long[] mat, boolean copy) {
        if (copy) {
            return new LongMatrix(Arrays.copyOf(mat, mat.length));
        } else {
            return new LongMatrix(mat);
        }
    }

    public static LongMatrix getInstance(LongMatrix m, boolean copy) {
        return getInstance(m.mat, copy);
    }

    public static LongMatrix ones() {
        long[] _mat = new long[64];
        for (int i = 0; i < _mat.length; i++) {
            _mat[i] = 1L << i;
        }
        return LongMatrix.getInstance(_mat, false);
    }

    public LongMatrix add(LongMatrix f) {
        if (this.rows != f.rows) {
            throw new IllegalRowCountException(
                    "The number of rows of this matrix must equal to the number of rows of another matrix.");
        }
        long[] _mat = new long[this.rows];
        for (int i = 0; i < this.rows; i++) {
            _mat[i] = this.mat[i] ^ f.mat[i];
        }
        return LongMatrix.getInstance(_mat, false);
    }

    public byte[] multiplyRight(long vector) {
        byte[] result = new byte[rows];
        for (int i = 0; i < rows; i++) {
            long v = mat[i] & vector;
            byte e = popCount(v);
            result[i] = e;
        }
        return result;
    }

    public LongMatrix multiplyRight(LongMatrix f) {
        return this.binary().multiplyRight(f);
    }

    public long multiplyLeft(byte[] vector) {
        long result = 0;
        for (int i = 0; i < rows; i++) {
            if (vector[i] == (byte) 1) {
                result ^= mat[i];
            }
        }
        return result;
    }

    public void swapRows(int i, int j) {
        long ri = mat[i];
        long rj = mat[j];
        mat[i] = rj;
        mat[j] = ri;
    }

    public void addRows(int src, int dst) {
        mat[dst] ^= mat[src];
    }

    /**
     * 階段行列を返す．thisに変更は加えない．
     * @return 
     */
    public Enchelon enchelon() {
        LongMatrix f = LongMatrix.getInstance(this, true);
        BinaryMatrix p = BinaryMatrix.ones(rows);
        int rank = 0;
        List<Integer> pivotsList = new ArrayList<>();
        for (int j = 0; j < 64; j++) {
            long b = 1L << j;
            for (int i = rank; i < f.rows; i++) {
                if ((f.mat[i] & b) != 0) {
                    /* erase other rows */
                    for (int k = 0; k < f.rows; k++) {
                        if ((k != i) && (f.mat[k] & b) != 0) {
                            f.addRows(i, k);
                            p.addRows(i, k);
                        }
                    }
                    f.swapRows(i, rank);
                    p.swapRows(i, rank);
                    pivotsList.add(j);
                    rank++;
                    break;
                }
            }
        }
        return new Enchelon(f, p, rank, pivotsList);
    }

    public BinaryMatrix generalizedInverse() {
        Enchelon e = enchelon();
        BinaryMatrix p = e.p;
        int rank = e.rank;
        List<Integer> pivots = e.pivots;
        BinaryMatrix permp = p.resized(this.columns);
        for (int i = rank - 1; i >= 0; i--) {
            int columnIndex = pivots.get(i);
            permp.swapRows(i, columnIndex);
        }
        return permp;
    }

    public long[] nullbasis() {
        BinaryMatrix g = generalizedInverse();
        LongMatrix h = g.multiplyRight(this).add(LongMatrix.ones());
        LongMatrix t = h.binary().transposed().longMatrix();
        Enchelon e = t.enchelon();
        return Arrays.copyOf(e.f.mat, e.rank);
    }

    public long[] nullspace() {
        long[] basis = nullbasis();
        long[] space = new long[1 << basis.length];
        for (int k = 0; k < (1 << basis.length); k++) {
            long v = 0;
            for (int l = 0; l < basis.length; l++) {
                if (((k >>> l) & 1) == 1) {
                    v ^= basis[l];
                }
            }
            space[k] = v;
        }
        return space;
    }

    public BinaryMatrix binary() {
        byte[][] _mat = new byte[this.rows][this.columns];
        for (int i = 0; i < this.rows; i++) {
            long r = this.mat[i];
            for (int j = 0; j < this.columns; j++) {
                _mat[i][j] = (byte) (r & 1);
                r >>>= 1;
            }
        }
        return BinaryMatrix.getInstance(this.rows, this.columns, _mat, false);
    }

    public VerboseLongMatrix verbose() {
        return new VerboseLongMatrix(Arrays.copyOf(mat, rows));
    }

    public static LongMatrix representPairwiseIntLinear(LongFunction<int[]> linear, int length) {
        byte[][] t = new byte[64][32 * length];
        for (int i = 0; i < 64; i++) {
            long seed = 1L << i;
            int[] a = linear.apply(seed);
            for (int j = 0; j < a.length; j++) {
                for (int k = 0; k < 32; k++) {
                    t[i][j * 32 + k] = (byte) ((a[j] >>> k) & 1);
                }
            }
        }
        return BinaryMatrix.getInstance(64, 32 * length, t, false).transposed().longMatrix();
    }

    public static LongMatrix representLongLinear(LongFunction<long[]> f, int length) {
        byte[][] t = new byte[64][64 * length];
        for (int i = 0; i < 64; i++) {
            long seed = 1L << i;
            long[] a = f.apply(seed);
            for (int j = 0; j < a.length; j++) {
                for (int k = 0; k < 64; k++) {
                    t[i][j * 64 + k] = (byte) ((a[j] >>> k) & 1);
                }
            }
        }
        return BinaryMatrix.getInstance(64, 64 * length, t, false).transposed().longMatrix();
    }

    /**
     * unsigned long を2進数表記したときの bit 1 の数を modulo 2 で返す．
     * @param x
     * @return {@code x} の2進数表記の bit 1 の数を 2 で割った余り．
     */
    public static byte popCount(long x) {
        x = x ^ (x >>> 32);
        x = x ^ (x >>> 16);
        x = x ^ (x >>> 8);
        x = x ^ (x >>> 4);
        x = x ^ (x >>> 2);
        x = x ^ (x >>> 1);
        return (byte) (x & 1);
    }

    public static class Enchelon {
        public final LongMatrix f;
        public final BinaryMatrix p;
        public final int rank;
        public final List<Integer> pivots;

        Enchelon(LongMatrix f, BinaryMatrix p, int rank, List<Integer> pivots) {
            this.f = f;
            this.p = p;
            this.rank = rank;
            this.pivots = pivots;
        }
    }
}
