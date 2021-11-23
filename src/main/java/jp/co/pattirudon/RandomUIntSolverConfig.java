package jp.co.pattirudon;

import java.util.List;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonProperty;

import jp.co.pattirudon.pokemon.Nature;

public class RandomUIntSolverConfig {
    @JsonProperty
    public UIntConfig primary, secondary;

    public static class UIntConfig {
        public int[] uints;

        @JsonProperty("uints")
        public void setUint(List<String> s) {
            List<Integer> t = s.stream().map(x -> Integer.parseUnsignedInt(x, 16)).toList();
            IntStream r = IntStream.range(0, t.size()).map(i -> t.get(i));
            this.uints = r.sorted().distinct().toArray();
        }

        public Nature nature;

        @JsonProperty("nature")
        public void setNature(String s) {
            if (s != null && s.length() > 0) {
                String name = capitalize(s);
                this.nature = Nature.valueOf(name);
            }
        }

        @JsonProperty
        public Integer frame;
        @JsonProperty
        public Integer frameStartInclusive, frameEndExclusive;

        private static String capitalize(final String line) {
            return Character.toUpperCase(line.charAt(0)) + line.substring(1);
        }
    }
}
