package jp.co.pattirudon;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RandomIVSolverConfig {
    @JsonProperty
    public IVConfig ivs;

    public static class IVConfig {
        @JsonProperty
        public int h, a, b, c, d, s;
    }
}
