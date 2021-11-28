package jp.co.pattirudon.swshrng.config;

import java.util.OptionalInt;

import com.fasterxml.jackson.annotation.JsonProperty;

import jp.co.pattirudon.swshrng.pokemon.Nature;

public class RandomUIntSolverConfig {
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

    public UIntConfig primary, secondary;

    public static class UIntConfig {
        public IVConfig ivs;
        public Nature nature;

        public void setNature(String name) {
            if (name != null && name.length() > 0)
                name = capitalize(name);
            this.nature = Nature.valueOf(name);
        }

        private static String capitalize(final String line) {
            return Character.toUpperCase(line.charAt(0)) + line.substring(1);
        }

        public FrameConfig frame;

        public UIntConfig(
                @JsonProperty(value = "ivs", required = true) IVConfig ivs,
                @JsonProperty(value = "nature", required = true) String nature,
                @JsonProperty(value = "frame", required = true) FrameConfig frame) {
            this.ivs = ivs;
            this.setNature(nature);
            this.frame = frame;
        }

    }

    public RandomUIntSolverConfig(
            @JsonProperty(value = "tsv", required = true) Integer tsv,
            @JsonProperty(value = "hasShinyCharm", required = true) boolean hasShinyCharm,
            @JsonProperty(value = "hasMarkCharm", required = true) boolean hasMarkCharm,
            @JsonProperty(value = "isWeatherActive", required = true) boolean isWeatherActive,
            @JsonProperty(value = "isFishing", required = true) boolean isFishing,
            @JsonProperty(value = "primary", required = true) UIntConfig primary,
            @JsonProperty(value = "secondary", required = true) UIntConfig secondary) {
        this.setTSV(tsv);
        this.hasShinyCharm = hasShinyCharm;
        this.hasMarkCharm = hasMarkCharm;
        this.isWeatherActive = isWeatherActive;
        this.isFishing = isFishing;
        this.primary = primary;
        this.secondary = secondary;
    }

}
