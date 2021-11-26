package jp.co.pattirudon.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ListMakerConfig {
    @JsonProperty
    public boolean isFishing = false;
    @JsonProperty
    public boolean isWeatherActive = true;
    @JsonProperty
    public boolean hasShinyCharm = false;
    @JsonProperty
    public boolean hasMarkCharm = false;
    @JsonProperty(required = true)
    public int tsv;
    @JsonProperty(required = true)
    public FrameConfig frame;
    public long seed;
    @JsonProperty("seed")
    public void setSeet(String s){
        this.seed = Long.parseUnsignedLong(s, 16);
    }
}
