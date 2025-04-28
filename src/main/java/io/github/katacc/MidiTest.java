package io.github.katacc;

import javax.sound.midi.*;
import java.util.Vector;

import static javax.sound.midi.MidiSystem.*;
import static javax.sound.midi.MidiSystem.getMidiDevice;
import static javax.sound.midi.MidiSystem.getTransmitter;

public class MidiTest {
    public static void midiTest() throws MidiUnavailableException {
        System.out.println("Awesome cli midi-mixer!");
        System.out.println("Your midi device: ");

        Vector<MidiDevice.Info> synthInfos = new Vector<>();
        MidiDevice mixer;

        MidiDevice.Info[] infos = getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {


            MidiDevice device = getMidiDevice(info);
            boolean isSequencer = device instanceof Sequencer;
            boolean isSynthesizer = device instanceof Synthesizer;

            try {
                if (!isSequencer && !isSynthesizer) {
                    synthInfos.add(info);
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }

            System.out.println(
                    "Device: " + device + ", " +
                            info.getName() + ", " +
                            info.getVendor() + ", " +
                            info.getVersion() + ", " +
                            info.getDescription() + ", " +
                            "sequencer = " + isSequencer +
                            ", synthesizer = " + isSynthesizer
            );

        }
        Receiver receiver = getReceiver();
        Transmitter transmitter = getTransmitter();

        System.out.println("Receiver: " + receiver);
        System.out.println("Transmitter: " + transmitter);

        mixer = getMidiDevice(synthInfos.getFirst());

        if (!(mixer.isOpen())) {
            try {
                mixer.open();
            } catch (MidiUnavailableException me) {
                System.out.println("Midi device unavailable: " + me.getMessage());
            }
        }

        System.out.println("\nDevices added to list:");
        for (MidiDevice.Info info : synthInfos) {
            System.out.println("Info: " + info);
        }

        System.out.println("Selected device: " + mixer);

        long mixer_timestamp = mixer.getMicrosecondPosition();

        System.out.println("Timestamp: " + mixer_timestamp);

        if (mixer.isOpen()) {
            mixer.close();
        }
        System.out.println("Mixer closed");

        receiver.close();
        transmitter.close();
    }
}
