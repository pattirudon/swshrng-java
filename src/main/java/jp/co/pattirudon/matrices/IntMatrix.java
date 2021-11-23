package jp.co.pattirudon.matrices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IntMatrix {
    public final int rows;
    public final int[] mat;
    public final int columns = 32;

    protected IntMatrix(int[] mat) {
        this.rows = mat.length;
        this.mat = mat;
    }

    public static IntMatrix getInstance(int[] mat, boolean copy) {
        if (copy) {
            return new IntMatrix(Arrays.copyOf(mat, mat.length));
        } else {
            return new IntMatrix(mat);
        }
    }

    public static IntMatrix getInstance(IntMatrix m, boolean copy) {
        return getInstance(m.mat, copy);
    }

    public static IntMatrix ones() {
        int[] _mat = new int[32];
        for (int i = 0; i < _mat.length; i++) {
            _mat[i] = 1 << i;
        }
        return IntMatrix.getInstance(_mat, false);
    }

    public IntMatrix add(IntMatrix f) {
        if (this.rows != f.rows) {
            throw new IllegalRowCountException(
                    "The number of rows of this matrix must equal to the number of rows of another matrix.");
        }
        int[] _mat = new int[this.rows];
        for (int i = 0; i < this.rows; i++) {
            _mat[i] = this.mat[i] ^ f.mat[i];
        }
        return IntMatrix.getInstance(_mat, false);
    }

    public byte[] multiplyRight(int vector) {
        byte[] result = new byte[rows];
        for (int i = 0; i < rows; i++) {
            int v = mat[i] & vector;
            byte e = popCount(v);
            result[i] = e;
        }
        return result;
    }

    public IntMatrix multiplyRight(IntMatrix f) {
        return this.binary().multiplyRight(f);
    }

    public int multiplyLeft(byte[] vector) {
        int result = 0;
        for (int i = 0; i < rows; i++) {
            if (vector[i] == (byte) 1) {
                result ^= mat[i];
            }
        }
        return result;
    }

    public void swapRows(int i, int j) {
        int ri = mat[i];
        int rj = mat[j];
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
        IntMatrix f = IntMatrix.getInstance(this, true);
        BinaryMatrix p = BinaryMatrix.ones(rows);
        int rank = 0;
        List<Integer> pivotsList = new ArrayList<>();
        for (int j = 0; j < 64; j++) {
            int b = 1 << j;
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

    public int[] nullbasis() {
        BinaryMatrix g = generalizedInverse();
        IntMatrix h = g.multiplyRight(this).add(IntMatrix.ones());
        IntMatrix t = h.binary().transposed().intMatrix();
        Enchelon e = t.enchelon();
        return Arrays.copyOf(e.f.mat, e.rank);
    }

    public int[] nullspace() {
        int[] basis = nullbasis();
        int[] space = new int[1 << basis.length];
        for (int k = 0; k < (1 << basis.length); k++) {
            int v = 0;
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
            int r = this.mat[i];
            for (int j = 0; j < this.columns; j++) {
                _mat[i][j] = (byte) (r & 1);
                r >>>= 1;
            }
        }
        return BinaryMatrix.getInstance(this.rows, this.columns, _mat, false);
    }

    public VerboseIntMatrix verbose() {
        return new VerboseIntMatrix(Arrays.copyOf(mat, rows));
    }

    /**
     * unsigned int を2進数表記したときの bit 1 の数を modulo 2 で返す．
     * @param x
     * @return {@code x} の2進数表記の bit 1 の数を 2 で割った余り．
     */
    public static byte popCount(int x) {
        x = x ^ (x >>> 16);
        x = x ^ (x >>> 8);
        x = x ^ (x >>> 4);
        x = x ^ (x >>> 2);
        x = x ^ (x >>> 1);
        return (byte) (x & 1);
    }

    public static class Enchelon {
        public final IntMatrix f;
        public final BinaryMatrix p;
        public final int rank;
        public final List<Integer> pivots;

        Enchelon(IntMatrix f, BinaryMatrix p, int rank, List<Integer> pivots) {
            this.f = f;
            this.p = p;
            this.rank = rank;
            this.pivots = pivots;
        }
    }
}
