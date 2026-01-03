package io.github.katacc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;


public class MidiInputReceiver implements Receiver {
    private static final Logger logger = LogManager.getLogger(MidiInputReceiver.class);
    public boolean skip = false;
    public String name;

    public MidiInputReceiver(String name) {
        this.name = name;
    }

    /**
     * Third bit = Value
     * Second bit = Control
     * The message is from 0 - 127
     * Control is from 0 - 23
     * For Korg midiKONTROL2, the faders go from 0 to 7 and the knobs go from 16 to 23
     */
    public void send(MidiMessage msg, long timeStamp) {
        byte[] bytes = msg.getMessage();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        logger.trace("Received MIDI message at {}: {}[ {}]", timeStamp, msg.getClass().getSimpleName(), sb.toString().trim());
        AudioController.getInstance().changeVolume(msg);
    }
    public void close() {

    }
}

