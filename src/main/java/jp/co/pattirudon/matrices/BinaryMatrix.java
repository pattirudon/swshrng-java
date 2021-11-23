package jp.co.pattirudon.matrices;

import java.util.Arrays;

public class BinaryMatrix {
    public final int rows;
    public final int columns;
    public final byte[][] mat;

    private BinaryMatrix(int rows, int columns, byte[][] mat) {
        this.rows = rows;
        this.columns = columns;
        this.mat = mat;
    }

    public static BinaryMatrix getInstance(int rows, int columns, byte[][] mat, boolean copy) {
        if (copy) {
            byte[][] _mat = new byte[rows][];
            for (int i = 0; i < rows; i++) {
                _mat[i] = Arrays.copyOf(mat[i], columns);
            }
            return new BinaryMatrix(rows, columns, _mat);
        } else {
            return new BinaryMatrix(rows, columns, mat);
        }
    }

    public static BinaryMatrix ones(int n) {
        byte[][] mat = new byte[n][n];
        for (int i = 0; i < n; i++) {
            mat[i][i] = (byte) 1;
        }
        BinaryMatrix o = new BinaryMatrix(n, n, mat);
        return o;
    }

    public IntMatrix multiplyRight(IntMatrix f) {
        if (this.columns != f.rows) {
            throw new IllegalColumnCountException(
                    "The number of columns of this matrix must equal to the number of rows of another matrix.");
        }
        int[] _mat = new int[this.rows];
        for (int i = 0; i < this.rows; i++) {
            _mat[i] = f.multiplyLeft(this.mat[i]);
        }
        return IntMatrix.getInstance(_mat, false);
    }

    public LongMatrix multiplyRight(LongMatrix f) {
        if (this.columns != f.rows) {
            throw new IllegalColumnCountException(
                    "The number of columns of this matrix must equal to the number of rows of another matrix.");
        }
        long[] _mat = new long[this.rows];
        for (int i = 0; i < this.rows; i++) {
            _mat[i] = f.multiplyLeft(this.mat[i]);
        }
        return LongMatrix.getInstance(_mat, false);
    }

    public BinaryMatrix transposed() {
        byte[][] t = new byte[columns][rows];
        for (int i = 0; i < columns; i++) {
            for (int j = 0; j < rows; j++) {
                t[i][j] = mat[j][i];
            }
        }
        return new BinaryMatrix(columns, rows, t);
    }

    public void swapRows(int i, int j) {
        byte[] ri = mat[i];
        byte[] rj = mat[j];
        mat[i] = rj;
        mat[j] = ri;
    }

    public void addRows(int src, int dst) {
        for (int j = 0; j < columns; j++) {
            mat[dst][j] ^= mat[src][j];
        }
    }

    public IntMatrix intMatrix() {
        if (columns == 32) {
            int[] r = new int[rows];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    r[i] |= (mat[i][j] & 1) << j;
                }
            }
            return IntMatrix.getInstance(r, false);
        } else {
            throw new IllegalColumnCountException("Cannot convert to an IntMatrix. The number of columns must be 32.");
        }
    }

    public LongMatrix longMatrix() {
        if (columns == 64) {
            long[] r = new long[rows];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    r[i] |= Integer.toUnsignedLong(mat[i][j] & 1) << j;
                }
            }
            return LongMatrix.getInstance(r, false);
        } else {
            throw new IllegalColumnCountException("Cannot convert to an LongMatrix. The number of columns must be 64.");
        }
    }

    public BinaryMatrix resized(int newRows) {
        byte[][] newMat = new byte[newRows][];
        for (int i = 0; i < this.rows; i++) {
            newMat[i] = Arrays.copyOf(this.mat[i], this.columns);
        }
        for (int i = this.rows; i < newRows; i++) {
            newMat[i] = new byte[this.columns];
        }
        return new BinaryMatrix(newRows, this.columns, newMat);
    }

}
