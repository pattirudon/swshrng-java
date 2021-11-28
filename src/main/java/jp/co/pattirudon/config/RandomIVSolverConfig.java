package jp.co.pattirudon.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RandomIVSolverConfig {
    public IVConfig ivs;

    public RandomIVSolverConfig(@JsonProperty(value = "ivs", required = true) IVConfig ivs) {
        this.ivs = ivs;
    }
}
