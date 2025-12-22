package io.github.katacc;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Transmitter;
import java.util.List;


public class MidiHandler {
    public static void midiHandler() {

        MidiDevice device;
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {
            try {
                device = MidiSystem.getMidiDevice(info);

                System.out.println(info);

                List<Transmitter> transmitters = device.getTransmitters();

                for (Transmitter transmitter : transmitters) {
                    transmitter.setReceiver(new MidiInputReceiver(device.getDeviceInfo().toString()));
                }

                if (device.getDeviceInfo().toString().contains("nanoKONTROL2")) {
                    device.open();
                }

                Transmitter trans = device.getTransmitter();
                trans.setReceiver(new MidiInputReceiver(device.getDeviceInfo().toString()));


                System.out.println(device.getDeviceInfo() + " Was opened");

            } catch (MidiUnavailableException me) {
                System.out.println("Error opening device: " + info);
                if (AudioController.getInstance().isDebug()) {
                    System.out.println(me);
                }
            }
        }

    }
}
