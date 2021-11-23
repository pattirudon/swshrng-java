package jp.co.pattirudon.matrices;

public class VerboseLongMatrix extends LongMatrix {

    public final long[][] leftMultiplied;
    public final int cells;
    public final int height = 16;

    VerboseLongMatrix(long[] mat) {
        super(mat);
        if (this.rows % this.height != 0) {
            throw new IllegalRowCountException("The number of rows must be a multiple of " + this.height + ".");
        }
        this.cells = this.rows / this.height;
        this.leftMultiplied = new long[cells][1 << height];
        precalculate();
    }

    private void precalculate() {
        for (int i = 0; i < cells; i++) {
            byte[] vector = new byte[rows];
            for (int y = 0; y < (1 << height); y++) {
                for (int j = 0; j < height; j++) {
                    vector[i * height + j] = (byte) ((y >>> j) & 1);
                }
                leftMultiplied[i][y] = multiplyLeft(vector);
            }
        }
    }

    public long multiplyLeft(int[] vector) {
        long result = 0;
        for (int i = 0; i < cells; i++) {
            result ^= leftMultiplied[i][vector[i]];
        }
        return result;
    }
}
