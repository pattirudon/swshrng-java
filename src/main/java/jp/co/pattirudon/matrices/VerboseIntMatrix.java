package jp.co.pattirudon.matrices;

public class VerboseIntMatrix extends IntMatrix {

    private final int[][] leftMultiplied;
    private final int cells;
    private final int height = 5;

    VerboseIntMatrix(int[] mat) {
        super(mat);
        if (this.rows % this.height != 0) {
            throw new IllegalRowCountException("The number of rows must be a multiple of " + this.height + ".");
        }
        this.cells = this.rows / this.height;
        this.leftMultiplied = new int[cells][1 << height];
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

    public int multiplyLeft(int[] vector) {
        int result = 0;
        for (int i = 0; i < cells; i++) {
            result ^= leftMultiplied[i][vector[i]];
        }
        return result;
    }
}
