package jp.co.pattirudon.swshrng.random;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import jp.co.pattirudon.swshrng.pokemon.Mark;
import jp.co.pattirudon.swshrng.pokemon.Nature;
import jp.co.pattirudon.swshrng.pokemon.NonPersonalityMark;
import jp.co.pattirudon.swshrng.pokemon.OverworldPokemon;
import jp.co.pattirudon.swshrng.pokemon.PersonalityMark;
import jp.co.pattirudon.swshrng.pokemon.Pokemon;

public class XoroshiroAdapter extends Xoroshiro {

    final OptionalInt tsv;
    final boolean hasShinyCharm;
    final boolean hasMarkCharm;
    final boolean isWeatherActive;
    final boolean isFishing;

    public XoroshiroAdapter(long seed, OptionalInt tsv, boolean hasShinyCharm, boolean hasMarkCharm,
            boolean isWeatherActive, boolean isFishing) {
        super(seed);
        this.tsv = tsv;
        this.hasShinyCharm = hasShinyCharm;
        this.hasMarkCharm = hasMarkCharm;
        this.isWeatherActive = isWeatherActive;
        this.isFishing = isFishing;
    }

    public static Pokemon fixedState(int seed, OptionalInt tsv, boolean willShiny) {
        Xoroshiro random = new Xoroshiro(Integer.toUnsignedLong(seed));
        int ec = random.nextInt();
        int pid = random.nextInt();
        if (tsv.isPresent()) {
            boolean isShinyPID = ((pid >>> 16) ^ (pid & 0xffff) ^ tsv.getAsInt()) < 0x10;
            if (willShiny && !isShinyPID) {
                int left = tsv.getAsInt() ^ (pid & 0xffff);
                int right = pid & 0xffff;
                pid = (left << 16) | right;
            } else if (!willShiny && isShinyPID) {
                pid ^= 0x10000000;
            }
        }
        String[] labels = { "h", "a", "b", "c", "d", "s" };
        Map<String, Integer> ivs = new LinkedHashMap<>(labels.length);
        for (int i = 0; i < labels.length; i++) {
            String l = labels[i];
            int iv = random.nextInt() & 0x1f;
            ivs.put(l, iv);
        }
        return new Pokemon(seed, ec, pid, ivs);
    }

    private static Optional<Mark> mark(Xoroshiro random, boolean isFishing, boolean isWeatherActive,
            boolean hasMarkCharm) {
        Optional<Mark> result = Optional.<Mark>empty();
        int rounds = hasMarkCharm ? 3 : 1;
        for (int i = 0; i < rounds; i++) {
            int rare = (int) random.random(1000);
            int personality = (int) random.random(100);
            int uncommon = (int) random.random(50);
            int weather = (int) random.random(50);
            int time = (int) random.random(50);
            int fishing = (int) random.random(25);
            if (rare == 0) {
                result = Optional.<Mark>of(NonPersonalityMark.Rare);
            } else if (personality == 0) {
                int len = PersonalityMark.values().length;
                int id = (int) random.random(len);
                Mark mark = PersonalityMark.valueOf(id);
                result = Optional.<Mark>of(mark);
            } else if (uncommon == 0) {
                result = Optional.<Mark>of(NonPersonalityMark.Uncommon);
            } else if (weather == 0 && isWeatherActive) {
                result = Optional.<Mark>of(NonPersonalityMark.Weather);
            } else if (time == 0) {
                result = Optional.<Mark>of(NonPersonalityMark.Time);
            } else if (fishing == 0 && isFishing) {
                result = Optional.<Mark>of(NonPersonalityMark.Fishing);
            }
            if (result.isPresent()) {
                break;
            }
        }
        return result;
    }

    public OverworldPokemon sampleOverworld() {
        Xoroshiro random = new Xoroshiro(this.s[0], this.s[1]);
        this.next();
        random.random(100);
        int rounds = hasShinyCharm ? 3 : 1;
        int mockPid = 0;
        boolean willShiny = false;
        if (this.tsv.isPresent()) {
            for (int i = 0; i < rounds; i++) {
                mockPid = random.nextInt();
                int x = (mockPid >>> 16) ^ (mockPid & 0xffff) ^ this.tsv.getAsInt();
                willShiny = (x & 0xf) == x;
                if (willShiny)
                    break;
            }
        } else {
            for (int i = 0; i < rounds; i++) {
                random.nextInt();
            }
        }
        random.random(2);
        int i;
        i = (int) random.random(25);
        Nature nature = Nature.valueOf(i);
        i = (int) random.random(2);
        int ability = new int[] { 2, 1 }[i];
        int seedLocal = random.nextInt();
        Pokemon p = fixedState(seedLocal, tsv, willShiny);
        Optional<Mark> m = mark(random, isFishing, isWeatherActive, hasMarkCharm);
        return p.extend(nature, ability, m);
    }
}
