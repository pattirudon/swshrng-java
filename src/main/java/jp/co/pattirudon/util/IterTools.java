package jp.co.pattirudon.util;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterTools {
    /**
     * https://stackoverflow.com/a/67183097/14865422
     */
    public static <U> List<List<U>> cartesianProduct(List<Set<? extends U>> sets) {
        // incorrect incoming data
        if (sets == null)
            return Collections.emptyList();
        return sets.stream()
                // non-null and non-empty sets
                .filter(set -> set != null && set.size() > 0)
                // represent each set element as Set<U>
                .map(set -> set.stream().map(List::<U>of)
                        // Stream<List<Set<U>>>
                        .collect(Collectors.toList()))
                // summation of pairs of inner sets
                .reduce((set1, set2) -> set1.stream()
                        // combinations of inner sets
                        .flatMap(inner1 -> set2.stream()
                                // merge two inner sets into one
                                .map(inner2 -> Stream.of(inner1, inner2).flatMap(List::stream)
                                        .collect(Collectors.toList())))
                        // list of combinations
                        .collect(Collectors.toList()))
                // List<Set<U>>
                .orElse(Collections.emptyList());
    }
}
