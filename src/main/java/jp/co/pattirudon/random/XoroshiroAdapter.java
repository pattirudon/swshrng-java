package jp.co.pattirudon.random;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import jp.co.pattirudon.pokemon.Mark;
import jp.co.pattirudon.pokemon.Nature;
import jp.co.pattirudon.pokemon.NonPersonalityMark;
import jp.co.pattirudon.pokemon.OverworldPokemon;
import jp.co.pattirudon.pokemon.Pokemon;

public class XoroshiroAdapter extends Xoroshiro {

    final boolean isFishing = false;
    final boolean isWeatherActive = true;
    final boolean hasShinyCharm = true;
    final boolean hasMarkCharm = true;
    final int tsv = 0;

    public XoroshiroAdapter(long seed) {
        super(seed);
    }

    public static Pokemon fixedState(int seed, int tsv, boolean willShiny) {
        Xoroshiro random = new Xoroshiro(Integer.toUnsignedLong(seed));
        int ec = random.nextInt();
        int pid = random.nextInt();
        boolean isShinyPID = ((pid >>> 16) ^ (pid & 0xffff) ^ tsv) < 0x10;
        if (willShiny && !isShinyPID) {
            int left = tsv ^ (pid & 0xffff);
            int right = pid & 0xffff;
            pid = (left << 16) | right;
        } else if (!willShiny && isShinyPID) {
            pid ^= 0x10000000;
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
        {
            random.random(100);
        }
        int rounds = hasShinyCharm ? 3 : 1;
        int mockPid = 0;
        boolean willShiny = false;
        for (int i = 0; i < rounds; i++) {
            mockPid = random.nextInt();
            int x = (mockPid >>> 16) ^ (mockPid & 0xffff) ^ this.tsv;
            willShiny = (x & 0xf) == x;
            if (willShiny)
                break;
        }
        {
            random.random(2);
        }
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
