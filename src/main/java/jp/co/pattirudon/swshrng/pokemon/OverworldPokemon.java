package jp.co.pattirudon.swshrng.pokemon;

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
        return String.format(f, nature, ability, mark.isPresent() ? mark.get().name() : "", seedLocal, ec, pid, ivs);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ability;
        result = prime * result + ((mark == null) ? 0 : mark.hashCode());
        result = prime * result + ((nature == null) ? 0 : nature.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        OverworldPokemon other = (OverworldPokemon) obj;
        if (ability != other.ability)
            return false;
        if (mark == null) {
            if (other.mark != null)
                return false;
        } else if (!mark.equals(other.mark))
            return false;
        if (nature != other.nature)
            return false;
        return true;
    }
}
