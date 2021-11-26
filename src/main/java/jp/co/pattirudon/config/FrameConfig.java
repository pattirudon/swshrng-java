package jp.co.pattirudon.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FrameConfig {
    @JsonProperty
    public int startInclusive, endExclusive;
}