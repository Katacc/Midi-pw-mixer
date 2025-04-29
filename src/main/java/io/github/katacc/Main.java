package io.github.katacc;


public class Main {
    public static void main(String[] args) throws InterruptedException {
        MidiHandler.midiHandler();
        AudioController controller = AudioController.getInstance();

        controller.getConfig();

    }
}
