package io.github.katacc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // First pass to handle log level early
        String logLevel = System.getProperty("logLevel", "info");
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-d") || arg.equals("--debug")) {
                logLevel = "debug";
                System.err.println("Warning: -d and --debug are deprecated. Use -l DEBUG instead.");
            } else if ((arg.equals("-l") || arg.equals("--log-level")) && i + 1 < args.length) {
                logLevel = args[++i];
            }
        }
        System.setProperty("logLevel", logLevel);
        // Reconfigure log4j2 to pick up the new system property
        ((LoggerContext) LogManager.getContext(false)).reconfigure();

        Logger logger = LogManager.getLogger(Main.class);
        AudioController controller = AudioController.getInstance();

        if (logLevel.equalsIgnoreCase("debug")) {
            logger.debug("Debug mode enabled");
        }

        for (int i = 0; i < args.length; i++) {
            if ((args[i].equals("-c") || args[i].equals("--cooldown")) && i + 1 < args.length) {
                try {
                    long cooldown = Long.parseLong(args[i + 1]);
                    controller.setConfigRefreshCooldownMs(cooldown);
                    logger.info("Cooldown set to {}ms", cooldown);
                    i++; // skip next arg
                } catch (NumberFormatException e) {
                    logger.error("Invalid cooldown value: {}", args[i + 1]);
                }
            }
        }

        MidiHandler.midiHandler();

        controller.getConfig();

        controller = null;
    }
}
