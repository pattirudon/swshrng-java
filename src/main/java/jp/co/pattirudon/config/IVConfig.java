package jp.co.pattirudon.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IVConfig {
    @JsonProperty(required = true)
    public int h, a, b, c, d, s;
}