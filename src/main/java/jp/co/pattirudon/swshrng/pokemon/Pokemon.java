package jp.co.pattirudon.swshrng.pokemon;

import java.util.Map;
import java.util.Optional;

public class Pokemon {
    public final int seedLocal;
    public final int ec, pid;
    public final Map<String, Integer> ivs;

    public Pokemon(int seedLocal, int ec, int pid, Map<String, Integer> ivs) {
        this.seedLocal = seedLocal;
        this.ec = ec;
        this.pid = pid;
        this.ivs = ivs;
    }

    public OverworldPokemon extend(Nature nature, int ability, Optional<Mark> mark) {
        return new OverworldPokemon(seedLocal, ec, pid, ivs, nature, ability, mark);
    }

    @Override
    public String toString() {
        String f = "localseed=%08x, ec=%08x, pid=%08x, ivs=%s";
        return String.format(f, seedLocal, ec, pid, ivs);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ec;
        result = prime * result + ((ivs == null) ? 0 : ivs.hashCode());
        result = prime * result + pid;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Pokemon other = (Pokemon) obj;
        if (ec != other.ec)
            return false;
        if (ivs == null) {
            if (other.ivs != null)
                return false;
        } else if (!ivs.equals(other.ivs))
            return false;
        if (pid != other.pid)
            return false;
        return true;
    }
}
