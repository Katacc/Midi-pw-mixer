package io.github.katacc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // First pass to handle debug flag early
        boolean debug = false;
        for (String arg : args) {
            if (arg.equals("-d") || arg.equals("--debug")) {
                debug = true;
                System.setProperty("logLevel", "debug");
                // Reconfigure log4j2 to pick up the new system property
                ((LoggerContext) LogManager.getContext(false)).reconfigure();
                break;
            }
        }

        Logger logger = LogManager.getLogger(Main.class);
        AudioController controller = AudioController.getInstance();

        if (debug) {
            logger.info("Debug mode enabled");
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
