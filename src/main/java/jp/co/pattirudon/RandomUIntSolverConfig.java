package jp.co.pattirudon;

import com.fasterxml.jackson.annotation.JsonProperty;

import jp.co.pattirudon.pokemon.Nature;

public class RandomUIntSolverConfig {
    @JsonProperty
    public UIntConfig primary, secondary;

    public static class UIntConfig {
        @JsonProperty("ivs")
        public IVConfig ivs;

        public Nature nature;

        @JsonProperty("nature")
        public void setNature(String s) {
            if (s != null && s.length() > 0) {
                String name = capitalize(s);
                this.nature = Nature.valueOf(name);
            }
        }

        private static String capitalize(final String line) {
            return Character.toUpperCase(line.charAt(0)) + line.substring(1);
        }

        @JsonProperty
        public FrameConfig frame;

        public static class FrameConfig {
            @JsonProperty
            public int startInclusive, endExclusive;
        }
    }
}
