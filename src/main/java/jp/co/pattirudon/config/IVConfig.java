package jp.co.pattirudon.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IVConfig {
    public int h, a, b, c, d, s;

    public IVConfig(@JsonProperty(value = "h", required = true) int h,
            @JsonProperty(value = "a", required = true) int a,
            @JsonProperty(value = "b", required = true) int b,
            @JsonProperty(value = "c", required = true) int c,
            @JsonProperty(value = "d", required = true) int d,
            @JsonProperty(value = "s", required = true) int s) {
        this.h = h;
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.s = s;
    }
}