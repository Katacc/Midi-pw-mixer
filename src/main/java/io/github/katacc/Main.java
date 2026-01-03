package io.github.katacc;


public class Main {
    public static void main(String[] args) throws InterruptedException {
        AudioController controller = AudioController.getInstance();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-d") || args[i].equals("--debug")) {
                controller.setDebug(true);
                System.out.println("Debug mode enabled");
            } else if ((args[i].equals("-c") || args[i].equals("--cooldown")) && i + 1 < args.length) {
                try {
                    long cooldown = Long.parseLong(args[i + 1]);
                    controller.setConfigRefreshCooldownMs(cooldown);
                    System.out.println("Cooldown set to " + cooldown + "ms");
                    i++; // skip next arg
                } catch (NumberFormatException e) {
                    System.err.println("Invalid cooldown value: " + args[i + 1]);
                }
            }
        }

        MidiHandler.midiHandler();

        controller.getConfig();

        controller = null;
    }
}
