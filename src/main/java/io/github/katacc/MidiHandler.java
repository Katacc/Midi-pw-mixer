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
        logger.info("Initializing MIDI handler...");
        MidiDevice device;
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        logger.debug("Found {} MIDI device(s)", infos.length);

        for (MidiDevice.Info info : infos) {
            try {
                device = MidiSystem.getMidiDevice(info);

                logger.debug("Found MIDI device: {} - {}", info.getName(), info.getDescription());

                List<Transmitter> transmitters = device.getTransmitters();
                logger.trace("Device {} has {} active transmitter(s)", info.getName(), transmitters.size());

                for (Transmitter transmitter : transmitters) {
                    transmitter.setReceiver(new MidiInputReceiver(device.getDeviceInfo().toString()));
                }

                if (device.getDeviceInfo().toString().contains("nanoKONTROL2")) {
                    logger.info("Target device found: {}. Opening...", info.getName());
                    device.open();
                } else {
                    logger.debug("Skipping non-target device: {}", info.getName());
                }

                Transmitter trans = device.getTransmitter();
                trans.setReceiver(new MidiInputReceiver(device.getDeviceInfo().toString()));


                logger.info("{} Was opened", device.getDeviceInfo());

            } catch (MidiUnavailableException me) {
                logger.error("Error opening device {}: {}", info.getName(), me.getMessage());
                logger.debug("Detailed exception for {}: ", info.getName(), me);
            }
        }
        logger.info("MIDI handler initialization finished.");
    }
}
