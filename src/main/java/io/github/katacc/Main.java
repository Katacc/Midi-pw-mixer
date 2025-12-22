package io.github.katacc;


public class Main {
    public static void main(String[] args) throws InterruptedException {
        AudioController controller = AudioController.getInstance();

        for (String arg : args) {
            if (arg.equals("-d") || arg.equals("--debug")) {
                controller.setDebug(true);
                System.out.println("Debug mode enabled");
            }
        }

        MidiHandler.midiHandler();

        controller.getConfig();

        controller = null;
    }
}
