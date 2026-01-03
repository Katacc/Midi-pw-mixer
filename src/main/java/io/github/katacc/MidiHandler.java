package io.github.katacc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Transmitter;
import java.util.List;


public class MidiHandler {
    private static final Logger logger = LogManager.getLogger(MidiHandler.class);

    public static void midiHandler() {

        MidiDevice device;
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {
            try {
                device = MidiSystem.getMidiDevice(info);

                logger.debug("Found MIDI device: {}", info);

                List<Transmitter> transmitters = device.getTransmitters();

                for (Transmitter transmitter : transmitters) {
                    transmitter.setReceiver(new MidiInputReceiver(device.getDeviceInfo().toString()));
                }

                if (device.getDeviceInfo().toString().contains("nanoKONTROL2")) {
                    device.open();
                }

                Transmitter trans = device.getTransmitter();
                trans.setReceiver(new MidiInputReceiver(device.getDeviceInfo().toString()));


                logger.info("{} Was opened", device.getDeviceInfo());

            } catch (MidiUnavailableException me) {
                logger.error("Error opening device: {}", info);
                logger.debug("Exception details:", me);
            }
        }

    }
}
