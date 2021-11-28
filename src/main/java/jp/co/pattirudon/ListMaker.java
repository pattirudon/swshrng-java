package jp.co.pattirudon;

import java.util.logging.Logger;

import jp.co.pattirudon.config.ListMakerConfig;
import jp.co.pattirudon.pokemon.OverworldPokemon;
import jp.co.pattirudon.random.XoroshiroAdapter;

public class ListMaker {
    public static void list(ListMakerConfig config, Logger logger) {
        XoroshiroAdapter random = new XoroshiroAdapter(config.seed, config.tsv, config.hasShinyCharm,
                config.hasMarkCharm, config.isWeatherActive, config.isFishing);
        for (int i = 0; i < config.frame.startInclusive; i++) {
            random.next();
        }
        for (int i = config.frame.startInclusive; i < config.frame.endExclusive; i++) {
            OverworldPokemon p = random.sampleOverworld();
            logger.info(String.format("%d: %s", i, p));
        }
    }
}
