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
     * For Korg midiKONTROL2, the faders go from 0 to 7
     */
    public void send(MidiMessage msg, long timeStamp) {
//         byte[] message = msg.getMessage();
//         System.out.println(Arrays.toString(message));
            AudioController.getInstance().changeVolume(msg);
    }
    public void close() {

    }
}

