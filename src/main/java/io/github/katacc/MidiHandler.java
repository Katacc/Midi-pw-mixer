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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MidiHandler {
    private static final Logger logger = LogManager.getLogger(MidiHandler.class);
    private static final List<Receiver> outputReceivers = new CopyOnWriteArrayList<>();
    private static final List<MidiDevice> openedDevices = new CopyOnWriteArrayList<>();
    private static final List<Transmitter> openedTransmitters = new CopyOnWriteArrayList<>();

    private static final Object connectionLock = new Object();
    private static ScheduledExecutorService scheduler;
    private static volatile boolean isConnected = false;
    private static String targetDeviceName = "nanoKONTROL2";
    private static long monitorIntervalSeconds = 2;
    private static long maxBackoffIntervalSeconds = 30;
    private static double backoffMultiplier = 2.0;
    private static long currentIntervalSeconds = 2;

    public static void midiHandler() {
        logger.info("Initializing MIDI handler...");
        initDevices();
        startConnectionMonitor();
    }

    public static boolean initDevices() {
        synchronized (connectionLock) {
            cleanupResources();

            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            logger.debug("Found {} MIDI device(s)", infos.length);

            boolean foundTarget = false;

            for (MidiDevice.Info info : infos) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(info);
                    logger.debug("Found MIDI device: {} - {}", info.getName(), info.getDescription());

                    String infoStr = device.getDeviceInfo().toString();
                    boolean isTarget = isTargetDevice(info, infoStr);

                    if (isTarget) {
                        logger.info("Target device found: {}. Opening...", info.getName());
                        device.open();
                        openedDevices.add(device);
                        foundTarget = true;

                        if (device.getMaxReceivers() != 0) {
                            try {
                                Receiver receiver = device.getReceiver();
                                outputReceivers.add(receiver);
                                logger.info("Acquired MIDI receiver for device: {}", info.getName());
                            } catch (MidiUnavailableException e) {
                                logger.debug("Device {} does not provide receiver: {}", info.getName(), e.getMessage());
                            }
                        }

                        if (device.getMaxTransmitters() != 0) {
                            try {
                                Transmitter trans = device.getTransmitter();
                                trans.setReceiver(new MidiInputReceiver(infoStr));
                                openedTransmitters.add(trans);
                                logger.info("Acquired MIDI transmitter for device: {}", info.getName());
                            } catch (MidiUnavailableException me) {
                                logger.debug("Device {} does not provide transmitter: {}", info.getName(), me.getMessage());
                            }
                        }

                        logger.info("{} Was opened", device.getDeviceInfo());
                    } else {
                        logger.debug("Skipping non-target device: {}", info.getName());
                    }

                } catch (MidiUnavailableException me) {
                    logger.error("Error opening device {}: {}", info.getName(), me.getMessage());
                    logger.debug("Detailed exception for {}: ", info.getName(), me);
                }
            }

            if (foundTarget && (!outputReceivers.isEmpty() || !openedTransmitters.isEmpty() || !openedDevices.isEmpty())) {
                isConnected = true;
                currentIntervalSeconds = monitorIntervalSeconds;
                logger.info("MIDI handler initialization finished. {} receiver(s) available for LED control.", outputReceivers.size());
                return true;
            } else {
                isConnected = false;
                logger.debug("Target MIDI device not found or could not be opened.");
                return false;
            }
        }
    }

    private static boolean isTargetDevice(MidiDevice.Info info, String infoStr) {
        if (targetDeviceName == null || targetDeviceName.isEmpty()) {
            return true;
        }
        return (info.getName() != null && info.getName().contains(targetDeviceName)) ||
                (info.getDescription() != null && info.getDescription().contains(targetDeviceName)) ||
                (infoStr != null && infoStr.contains(targetDeviceName));
    }

    public static boolean isTargetDevicePresent() {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {
            if (isTargetDevice(info, info.toString())) {
                return true;
            }
        }
        return false;
    }

    public static void checkConnection() {
        synchronized (connectionLock) {
            boolean targetPresent = isTargetDevicePresent();
            boolean wasConnected = isConnected;

            if (wasConnected) {
                boolean anyDeviceClosed = false;
                for (MidiDevice d : openedDevices) {
                    if (!d.isOpen()) {
                        anyDeviceClosed = true;
                        break;
                    }
                }

                if (!targetPresent || anyDeviceClosed) {
                    logger.warn("MIDI device connection lost (targetPresent={}, anyDeviceClosed={}). Triggering disconnect handling.",
                            targetPresent, anyDeviceClosed);
                    handleDisconnect();
                }
            } else {
                if (targetPresent) {
                    logger.info("Target MIDI device detected. Attempting to reconnect...");
                    boolean connected = initDevices();
                    if (connected) {
                        logger.info("Successfully reconnected to MIDI device.");
                        currentIntervalSeconds = monitorIntervalSeconds;
                        try {
                            AudioController.getInstance().syncButtonLedStates();
                        } catch (Exception e) {
                            logger.error("Failed to sync button LED states after reconnect: {}", e.getMessage());
                        }
                    } else {
                        currentIntervalSeconds = calculateNextBackoff(currentIntervalSeconds);
                        logger.debug("Reconnection attempt failed. Next retry in {}s.", currentIntervalSeconds);
                    }
                } else {
                    currentIntervalSeconds = calculateNextBackoff(currentIntervalSeconds);
                    logger.debug("Target MIDI device not present. Next check in {}s.", currentIntervalSeconds);
                }
            }
        }
    }

    public static long calculateNextBackoff(long current) {
        long next = Math.round(current * backoffMultiplier);
        if (next > maxBackoffIntervalSeconds) {
            next = maxBackoffIntervalSeconds;
        }
        if (next < monitorIntervalSeconds) {
            next = monitorIntervalSeconds;
        }
        return next;
    }

    public static void handleDisconnect() {
        synchronized (connectionLock) {
            if (!isConnected && outputReceivers.isEmpty() && openedDevices.isEmpty() && openedTransmitters.isEmpty()) {
                return;
            }
            logger.warn("Handling MIDI disconnect. Cleaning up handles...");
            cleanupResources();
            isConnected = false;
            currentIntervalSeconds = monitorIntervalSeconds;
        }
    }

    private static void cleanupResources() {
        for (Receiver r : outputReceivers) {
            closeQuietly(r);
        }
        outputReceivers.clear();
        for (Transmitter t : openedTransmitters) {
            closeQuietly(t);
        }
        openedTransmitters.clear();

        for (MidiDevice d : openedDevices) {
            closeQuietly(d);
        }
        openedDevices.clear();
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                logger.debug("Error closing MIDI resource: {}", e.getMessage());
            }
        }
    }

    public static synchronized void startConnectionMonitor() {
        if (scheduler == null || scheduler.isShutdown() || scheduler.isTerminated()) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "midi-connection-monitor");
                t.setDaemon(false);
                return t;
            });
            scheduleNextCheck(monitorIntervalSeconds);
            logger.info("MIDI connection monitor started (interval: {}s).", monitorIntervalSeconds);
        }
    }

    private static synchronized void scheduleNextCheck(long delaySeconds) {
        if (scheduler != null && !scheduler.isShutdown() && !scheduler.isTerminated()) {
            try {
                scheduler.schedule(() -> {
                    long nextDelay = monitorIntervalSeconds;
                    try {
                        checkConnection();
                        synchronized (connectionLock) {
                            nextDelay = currentIntervalSeconds;
                        }
                    } catch (Exception e) {
                        logger.error("Error during MIDI connection check: {}", e.getMessage(), e);
                    } finally {
                        scheduleNextCheck(nextDelay);
                    }
                }, delaySeconds, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.debug("Failed to schedule next connection check: {}", e.getMessage());
            }
        }
    }

    public static synchronized void stopConnectionMonitor() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
            logger.info("MIDI connection monitor stopped.");
        }
    }

    public static boolean isConnected() {
        return isConnected;
    }

    public static void setConnected(boolean connected) {
        isConnected = connected;
    }

    public static String getTargetDeviceName() {
        return targetDeviceName;
    }

    public static void setTargetDeviceName(String name) {
        targetDeviceName = name;
    }

    public static long getMonitorIntervalSeconds() {
        return monitorIntervalSeconds;
    }

    public static void setMonitorIntervalSeconds(long intervalSeconds) {
        synchronized (connectionLock) {
            monitorIntervalSeconds = Math.max(1, intervalSeconds);
            if (maxBackoffIntervalSeconds < monitorIntervalSeconds) {
                maxBackoffIntervalSeconds = monitorIntervalSeconds;
            }
            if (isConnected || currentIntervalSeconds < monitorIntervalSeconds) {
                currentIntervalSeconds = monitorIntervalSeconds;
            }
        }
    }

    public static long getMaxBackoffIntervalSeconds() {
        synchronized (connectionLock) {
            return maxBackoffIntervalSeconds;
        }
    }

    public static void setMaxBackoffIntervalSeconds(long maxIntervalSeconds) {
        synchronized (connectionLock) {
            maxBackoffIntervalSeconds = Math.max(monitorIntervalSeconds, maxIntervalSeconds);
        }
    }

    public static double getBackoffMultiplier() {
        synchronized (connectionLock) {
            return backoffMultiplier;
        }
    }

    public static void setBackoffMultiplier(double multiplier) {
        synchronized (connectionLock) {
            if (multiplier >= 1.0) {
                backoffMultiplier = multiplier;
            }
        }
    }

    public static long getCurrentIntervalSeconds() {
        synchronized (connectionLock) {
            return currentIntervalSeconds;
        }
    }

    public static void setCurrentIntervalSeconds(long intervalSeconds) {
        synchronized (connectionLock) {
            currentIntervalSeconds = intervalSeconds;
        }
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
                    handleDisconnect();
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

    public static List<MidiDevice> getOpenedDevices() {
        return openedDevices;
    }

    public static List<Transmitter> getOpenedTransmitters() {
        return openedTransmitters;
    }
}
