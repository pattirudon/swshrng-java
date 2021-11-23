package jp.co.pattirudon.random;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import jp.co.pattirudon.pokemon.OverworldPokemon;
import jp.co.pattirudon.pokemon.Pokemon;

public class XoroshiroAdapterTest {

    @Test
    public void testFixedState() {
        int seed = 0xef021648;
        int tsv = 0;
        boolean willShiny = false;
        Pokemon actual = XoroshiroAdapter.fixedState(seed, tsv, willShiny);
        int[] expected_iv_array = { 9, 9, 15, 19, 19, 13 };
        Map<String, Integer> expected_iv_map = new LinkedHashMap<>();
        String[] labels = { "h", "a", "b", "c", "d", "s" };
        for (int i = 0; i < labels.length; i++) {
            expected_iv_map.put(labels[i], expected_iv_array[i]);
        }
        Pokemon expected = new Pokemon(seed, 0x1e091bdb, 0x36d7c188, expected_iv_map);
        assertEquals(expected, actual);
    }

    @Test
    public void testSampleOverworld(){
        long seed=0x4804dc0a68cc0c40L;
        XoroshiroAdapter random = new XoroshiroAdapter(seed);
        for (int i = 0; i < 24; i++) {
            OverworldPokemon p = random.sampleOverworld();
            System.out.println(p);
        }
    }
}
