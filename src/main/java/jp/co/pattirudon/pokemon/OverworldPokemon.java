package jp.co.pattirudon.pokemon;

import java.util.Map;
import java.util.Optional;

public class OverworldPokemon extends Pokemon {
    final public Nature nature;
    final public int ability;
    final public Optional<Mark> mark;

    public OverworldPokemon(int seedLocal, int ec, int pid, Map<String, Integer> ivs, Nature nature, int ability,
            Optional<Mark> mark) {
        super(seedLocal, ec, pid, ivs);
        this.nature = nature;
        this.ability = ability;
        this.mark = mark;
    }

    @Override
    public String toString() {
        String f = "nature=%s, ability=%d, mark=%s, localseed=%08x, ec=%08x, pid=%08x, ivs=%s";
        return String.format(f, nature, ability, mark, seedLocal, ec, pid, ivs);
    }
}
