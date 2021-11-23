package jp.co.pattirudon.util;

import java.util.List;
import java.util.Set;

import org.junit.Test;

public class IterToolsTest {
    @Test
    public void testCartesianProduct() {
        Set<Integer> set1 = Set.of(1, 2);
        Set<Double> set2 = Set.of(3.0, 4.0);
        Set<Long> set3 = Set.of(5L, 6L);

        List<List<Number>> sets = IterTools.cartesianProduct(List.of(set1, set2, set3));
        // output
        sets.forEach(System.out::println);
    }
}
