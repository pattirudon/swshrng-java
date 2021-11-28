package jp.co.pattirudon.swshrng;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import jp.co.pattirudon.swshrng.config.ListMakerConfig;
import jp.co.pattirudon.swshrng.config.RandomIVSolverConfig;
import jp.co.pattirudon.swshrng.config.RandomUIntSolverConfig;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(version = "1.0", description = "Overworld prng seed determination tool for pokemon sword/shield", mixinStandardHelpOptions = true)
public class Main implements Callable<Integer> {
    @ArgGroup(heading = "Actions:\n", exclusive = true, multiplicity = "1")
    Actions actions;

    @Parameters(paramLabel = "PATH", description = "Path to a config json file.")
    Path configFilePath;

    static class Actions {
        @Option(names = {
                "local" }, required = true, description = "Search for 32-bit seeds of the local xoroshiro. "
                        + "In game generated ivs of a single pokemon are required.")
        boolean local;
        @Option(names = {
                "global" }, required = true, description = "Search for 64-bit seeds of the global xoroshiro. "
                        + "In game generated ivs of two pokemons are required.")
        boolean global;
        @Option(names = {
                "list" }, required = true, description = "Output information of pokemons possibly generated in the overworld."
                        + "A 64-bit seed of the global xoroshiro is required.")
        boolean list;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        Logger logger = getLogger("result.%g.log");
        long start = System.currentTimeMillis();
        InputStream is = Files.newInputStream(configFilePath);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true);
        if (actions.local) {
            RandomIVSolverConfig config = mapper.readValue(is, RandomIVSolverConfig.class);
            RandomIVSolver.list(config, logger);
        } else if (actions.global) {
            RandomUIntSolverConfig config = mapper.readValue(is, RandomUIntSolverConfig.class);
            RandomUIntSolver.list(config, logger);
        } else if (actions.list) {
            ListMakerConfig config = mapper.readValue(is, ListMakerConfig.class);
            ListMaker.list(config, logger);
        }
        long end = System.currentTimeMillis();
        logger.config("Finish. [%d ms]".formatted(end - start));
        return 0;
    }

    public static Logger getLogger(String fileName) throws IOException {
        Logger logger = Logger.getLogger(Main.class.getName());
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.CONFIG);
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new BareFormatter());
        consoleHandler.setLevel(Level.CONFIG);
        logger.addHandler(consoleHandler);
        String dirName = "log";
        Files.createDirectories(Path.of(dirName));
        boolean append = true;
        Handler fileHandler = new FileHandler(Path.of(dirName, fileName).toString(), append);
        fileHandler.setFormatter(new BareFormatter());
        fileHandler.setLevel(Level.INFO);
        logger.addHandler(fileHandler);
        return logger;
    }

    final static class BareFormatter extends Formatter {
        @Override
        public synchronized String format(LogRecord aRecord) {
            final StringBuffer message = new StringBuffer();
            message.append(formatMessage(aRecord));
            message.append(String.format("%n"));
            return message.toString();
        }
    }
}
