package jp.co.pattirudon.util;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

public class IterToolsTest {
    @Test
    public void testCartesianProduct() {
        Set<Integer> set1 = new TreeSet<>(List.of(1, 2));
        Set<Double> set2 = new TreeSet<>(List.of(3.0, 4.0));
        Set<Long> set3 = new TreeSet<>(List.of(5L, 6L));

        List<List<Number>> actual = IterTools.cartesianProduct(List.of(set1, set2, set3));

        List<List<Number>> expected = List.of(
                List.<Number>of(1, 3.0, 5L), List.<Number>of(1, 3.0, 6L), List.<Number>of(1, 4.0, 5L),
                List.<Number>of(1, 4.0, 6L), List.<Number>of(2, 3.0, 5L), List.<Number>of(2, 3.0, 6L),
                List.<Number>of(2, 4.0, 5L), List.<Number>of(2, 4.0, 6L));

        assertEquals(actual, expected);
    }
}
