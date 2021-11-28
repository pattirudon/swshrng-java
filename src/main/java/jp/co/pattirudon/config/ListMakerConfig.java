package jp.co.pattirudon.config;

import java.util.OptionalInt;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ListMakerConfig {
    public OptionalInt tsv;

    public void setTSV(Integer tsv) {
        if (tsv == null) {
            this.tsv = OptionalInt.empty();
        } else {
            this.tsv = OptionalInt.of(tsv.intValue());
        }
    }

    public boolean hasShinyCharm;
    public boolean hasMarkCharm;
    public boolean isWeatherActive;
    public boolean isFishing;

    public long seed;

    public void setSeed(String s) {
        this.seed = Long.parseUnsignedLong(s, 16);
    }

    public FrameConfig frame;

    public ListMakerConfig(
            @JsonProperty(value = "tsv", required = true) Integer tsv,
            @JsonProperty(value = "hasShinyCharm", required = true) boolean hasShinyCharm,
            @JsonProperty(value = "hasMarkCharm", required = true) boolean hasMarkCharm,
            @JsonProperty(value = "isWeatherActive", required = true) boolean isWeatherActive,
            @JsonProperty(value = "isFishing", required = true) boolean isFishing,
            @JsonProperty(value = "seed", required = true) String seed,
            @JsonProperty(value = "frame", required = true) FrameConfig frame) {
        this.isFishing = isFishing;
        this.isWeatherActive = isWeatherActive;
        this.hasShinyCharm = hasShinyCharm;
        this.hasMarkCharm = hasMarkCharm;
        this.setTSV(tsv);
        this.setSeed(seed);
        this.frame = frame;
    }

}
