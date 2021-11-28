package jp.co.pattirudon.swshrng.matrices;

import static org.junit.Assert.assertArrayEquals;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class IntMatrixTest {
    @Test
    public void testGeneralizedInverse() {
        String s = "Qvi7BwfAtajJlbF4ewcTgOMvkkfEICJ7ZKMkbogcrqLfarZQca72jYNBmdVC"
                + "PPwkYYWhmbJ9H4Q8m6syaKNPGn2q01PjFfiVdbFhfAsYlJctHEzRVKFlpLx5";
        ByteBuffer bb = ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
        int[] mat = new int[30];
        for (int i = 0; i < mat.length; i++) {
            mat[i] = bb.getInt();
        }
        IntMatrix I = IntMatrix.getInstance(mat, false);
        BinaryMatrix J = I.generalizedInverse();
        IntMatrix IJI = I.multiplyRight(J.multiplyRight(I));
        assertArrayEquals(I.mat, IJI.mat);
    }
}
