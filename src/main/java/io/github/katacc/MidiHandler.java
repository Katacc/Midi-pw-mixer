package io.github.katacc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class MidiHandler {
    private static final Logger logger = LogManager.getLogger(MidiHandler.class);
    private static final List<Receiver> outputReceivers = new CopyOnWriteArrayList<>();

    public static void midiHandler() {
        logger.info("Initializing MIDI handler...");
        outputReceivers.clear();
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
                    if (device.getMaxReceivers() != 0) {
                        try {
                            Receiver receiver = device.getReceiver();
                            outputReceivers.add(receiver);
                            logger.info("Acquired MIDI receiver for device: {}", info.getName());
                        } catch (MidiUnavailableException e) {
                            logger.debug("Device {} does not provide receiver: {}", info.getName(), e.getMessage());
                        }
                    }
                } else {
                    logger.debug("Skipping non-target device: {}", info.getName());
                }

                if (device.getMaxTransmitters() != 0) {
                    try {
                        Transmitter trans = device.getTransmitter();
                        trans.setReceiver(new MidiInputReceiver(device.getDeviceInfo().toString()));
                    } catch (MidiUnavailableException me) {
                        logger.debug("Device {} does not provide transmitter: {}", info.getName(), me.getMessage());
                    }
                }

                logger.info("{} Was opened", device.getDeviceInfo());

            } catch (MidiUnavailableException me) {
                logger.error("Error opening device {}: {}", info.getName(), me.getMessage());
                logger.debug("Detailed exception for {}: ", info.getName(), me);
            }
        }
        logger.info("MIDI handler initialization finished. {} receiver(s) available for LED control.", outputReceivers.size());
    }

    public static void setButtonLed(int controlNumber, boolean on) {
        setButtonLed(controlNumber, on ? 127 : 0);
    }

    public static void setButtonLed(int controlNumber, int value) {
        logger.debug("Setting LED for control {} to value {}", controlNumber, value);
        try {
            ShortMessage msg = new ShortMessage();
            msg.setMessage(ShortMessage.CONTROL_CHANGE, 0, controlNumber, value);
            for (Receiver receiver : outputReceivers) {
                try {
                    receiver.send(msg, -1);
                } catch (Exception e) {
                    logger.error("Failed to send LED MIDI message to receiver: {}", e.getMessage());
                }
            }
        } catch (InvalidMidiDataException e) {
            logger.error("Failed to construct MIDI message for control {}: {}", controlNumber, e.getMessage());
        }
    }

    public static void addReceiver(Receiver receiver) {
        outputReceivers.add(receiver);
    }

    public static void clearReceivers() {
        outputReceivers.clear();
    }

    public static List<Receiver> getReceivers() {
        return outputReceivers;
    }
}
