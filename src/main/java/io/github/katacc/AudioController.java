package io.github.katacc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;

import javax.sound.midi.MidiMessage;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


public class AudioController {
    private static final Logger logger = LogManager.getLogger(AudioController.class);

    private static AudioController single_instance = null;

    private final List<Integer> id0;
    private final List<Integer> id1;
    private final List<Integer> id2;
    private final List<Integer> id3;
    private final List<Integer> id4;
    private final List<Integer> id5;
    private final List<Integer> id6;
    private final List<Integer> id7;
    private final List<Integer> id16;
    private final List<Integer> id17;
    private final List<Integer> id18;
    private final List<Integer> id19;
    private final List<Integer> id20;
    private final List<Integer> id21;
    private final List<Integer> id22;
    private final List<Integer> id23;

    private Vector<String> id7App;
    private Vector<String> id16App;
    private Vector<String> id17App;
    private Vector<String> id18App;
    private Vector<String> id19App;
    private Vector<String> id20App;
    private Vector<String> id21App;
    private Vector<String> id22App;
    private Vector<String> id23App;
    private Vector<String> id5App;
    private Vector<String> id6App;
    private Vector<String> id4App;
    private Vector<String> id3App;
    private Vector<String> id2App;
    private Vector<String> id1App;
    private Vector<String> id0App;

    // Variable to not set the volume on every single message to avoid lag in certain situtations
    private int audioSetTimer;

    // Throttling updates: capturing only the latest volume (not yet executed) per control
    // https://medium.com/@anuragnitdgr/resolving-race-conditions-in-java-with-concurrenthashmap-and-atomicboolean-583d3e31b52c
    private final ConcurrentHashMap<Integer, AtomicReference<Float>> pendingVolume = new ConcurrentHashMap<>();
    private final AtomicBoolean volumeWorkerRunning = new AtomicBoolean(false);
    // Protect ID/Config Updates
    private final Object configLock = new Object();
    private long lastConfigRefresh = 0;
    private long configRefreshCooldownMs = 10000; // default 10 seconds cooldown

    private AudioController() {

        id0 = new ArrayList<>();
        id1 = new ArrayList<>();
        id2 = new ArrayList<>();
        id3 = new ArrayList<>();
        id4 = new ArrayList<>();
        id5 = new ArrayList<>();
        id6 = new ArrayList<>();
        id7 = new ArrayList<>();
        id16 = new ArrayList<>();
        id17 = new ArrayList<>();
        id18 = new ArrayList<>();
        id19 = new ArrayList<>();
        id20 = new ArrayList<>();
        id21 = new ArrayList<>();
        id22 = new ArrayList<>();
        id23 = new ArrayList<>();


        id0App = new Vector<>();
        id1App = new Vector<>();
        id2App = new Vector<>();
        id3App = new Vector<>();
        id4App = new Vector<>();
        id5App = new Vector<>();
        id6App = new Vector<>();
        id7App = new Vector<>();
        id16App = new Vector<>();
        id17App = new Vector<>();
        id18App = new Vector<>();
        id19App = new Vector<>();
        id20App = new Vector<>();
        id21App = new Vector<>();
        id22App = new Vector<>();
        id23App = new Vector<>();

        audioSetTimer = 0;
    }

    public static AudioController getInstance() {
        if (single_instance == null) {
            single_instance = new AudioController();
        }
        return single_instance;
    }

    public long getConfigRefreshCooldownMs() {
        return configRefreshCooldownMs;
    }

    public void setConfigRefreshCooldownMs(long configRefreshCooldownMs) {
        this.configRefreshCooldownMs = configRefreshCooldownMs;
    }

    public void changeVolume(MidiMessage msg) {

        byte[] message = msg.getMessage();

        logger.debug("Raw MIDI message: {}", Arrays.toString(message));

        byte control = message[1];
        byte value = message[2];

        float scaled_volume = (((float) value / 127) * 100) / 100;

        if (audioSetTimer >= 1) {
            audioSetTimer = 0;
            logger.debug("Control: {} value={} scaled={}", control, value, scaled_volume);
            // Volume controls
            if (isVolumeControl(control)) {
                // Volume controls are high frequency and we like to skip non-applied, non-final volume changes for
                // faster reaction). Therefore we queue them instead of executing them directly.
                // Actual execution happens ansynchronously in volumeWorkerLoop() (applyVolumeToControl())
                queueVolumeUpdate(control, scaled_volume);
            }
        }
        audioSetTimer++;

        // Media controls (keep synchronous; these are low frequency)
        if (control == 41 && value == 127) {
            runPlayerctl("play-pause");
        }
        if (control == 44 && value == 127) {
            runPlayerctl("next");
        }
        if (control == 43 && value == 127) {
            runPlayerctl("previous");
        }
        if (control == 42 && value == 127) {
            runPlayerctl("stop");
        }
        if (control == 46 && value == 127) {
            synchronized (configLock) {
                getConfigUnsafe();
            }
            logger.info("Forced reconfig");
        }
    }

    private boolean isVolumeControl(int control) {
        // volume controls are 0-7 (faders) and 16-23 (knobs)
        return (control >= 0 && control <= 7) || (control >= 16 && control <= 23);
    }

    private void queueVolumeUpdate(int control, float scaled_volume) {
        pendingVolume.computeIfAbsent(control, k -> new AtomicReference<>()).set(scaled_volume);

        if (volumeWorkerRunning.compareAndSet(false, true)) {
            new Thread(this::volumeWorkerLoop, "wpctl-volume-worker").start();
        }
    }

    private void volumeWorkerLoop() {
        try {
            while (true) {
                Integer nextControl = findAnyPendingControl();
                if (nextControl == null) {
                    return;
                }

                AtomicReference<Float> ref = pendingVolume.get(nextControl);
                if (ref == null) {
                    continue;
                }

                Float volume = ref.getAndSet(null);
                if (volume == null) {
                    continue;
                }

                applyVolumeToControl(nextControl, volume);
            }
        } finally {
            volumeWorkerRunning.set(false);

            // If something arrived after we decided to stop, restart.
            if (findAnyPendingControl() != null && volumeWorkerRunning.compareAndSet(false, true)) {
                new Thread(this::volumeWorkerLoop, "wpctl-volume-worker").start();
            }
        }
    }

    private Integer findAnyPendingControl() {
        for (Map.Entry<Integer, AtomicReference<Float>> entry : pendingVolume.entrySet()) {
            if (entry.getValue().get() != null) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void applyVolumeToControl(int control, float scaled_volume) {
        List<Integer> idsSnapshot;

        synchronized (configLock) {
            List<Integer> ids = getIdsForVolumeControl(control);
            if (ids == null) {
                return;
            }

            if (ids.isEmpty()) {
                if (System.currentTimeMillis() - lastConfigRefresh > configRefreshCooldownMs) {
                    logger.info("id{} is empty... refreshing config... throttling for {}ms", control, configRefreshCooldownMs);
                    getConfigUnsafe();
                    ids = getIdsForVolumeControl(control);
                    if (ids == null) {
                        return;
                    }
                } else {
                    logger.debug("id{} is empty, but cooldown in effect. Skipping refresh.", control);
                }
            }

            idsSnapshot = new ArrayList<>(ids);
        }

        for (int id : idsSnapshot) {
            runWpctlSetVolume(id, scaled_volume);
        }
    }

    private List<Integer> getIdsForVolumeControl(int control) {
        switch (control) {
            // Faders
            case 0:  return id0;
            case 1:  return id1;
            case 2:  return id2;
            case 3:  return id3;
            case 4:  return id4;
            case 5:  return id5;
            case 6:  return id6;
            case 7:  return id7;
            // Knobs
            case 16: return id16;
            case 17: return id17;
            case 18: return id18;
            case 19: return id19;
            case 20: return id20;
            case 21: return id21;
            case 22: return id22;
            case 23: return id23;
            // Other controls are no volume controls
            default: return null;
        }
    }

    private void runWpctlSetVolume(int id, float scaled_volume) {
        try {
            ProcessBuilder pb = new ProcessBuilder("wpctl", "set-volume", String.valueOf(id), String.valueOf(scaled_volume));
            logger.debug("Executing command: {}", String.join(" ", pb.command()));
            Process process = pb.start();
            int returnCode = process.waitFor(); // prevent process pile-up
            if (returnCode != 0) {
                logger.error("wpctl exited with return code={}", returnCode);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error setting volume: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private void runPlayerctl(String action) {
        try {
            ProcessBuilder pb = new ProcessBuilder("playerctl", action);
            logger.debug("Executing command: {}", String.join(" ", pb.command()));
            Process process = pb.start();
            int returnCode = process.waitFor();
            if (returnCode != 0) {
                logger.error("playerctl exited with return code={}", returnCode);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error running playerctl: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * getId to get id of a application from pw-dump
     * */
    public List<Integer> getId(String name) {
        long startTime = System.currentTimeMillis();
        logger.debug("Looking up ID for: {}", name);

        String targetName = name;
        List<Integer> appId = new ArrayList<>();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder("pw-dump");
            Process process = processBuilder.start();

            // Reading the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder jsonOutput = new StringBuilder();
            String line = "";

            while ((line = reader.readLine()) != null) {
                jsonOutput.append(line);
            }

            // Parse the JSON output
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonOutput.toString());

            for (JsonNode node : rootNode) {
                if (node.has("type") && "PipeWire:Interface:Node".equals(node.get("type").asText())) {
                    JsonNode props = node.path("info").path("props");
                    String nodeName = props.path("node.name").asText("");

                    if (nodeName.equalsIgnoreCase(targetName)) {
                        appId.add(node.path("id").asInt());
                    }
                }
            }

        } catch (IOException e) {
            logger.error("Error running pw-dump: {}", e.getMessage());
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.debug("Found IDs for {}: {} (took {}ms)", name, appId, duration);

        return appId;
    }

    /**
     * Thread-safe entry point: uses configLock.
     */
    public void getConfig() {
        synchronized (configLock) {
            getConfigUnsafe();
        }
    }

    /**
     * Internal implementation; caller must hold configLock.
     */
    private void getConfigUnsafe() {
        lastConfigRefresh = System.currentTimeMillis();
        logger.info("Refreshing config...");
        long startTime = System.currentTimeMillis();

        // Path to config file
        String userHome = System.getProperty("user.home");
        String configPath = userHome + "/.config/midi-mixer/config.ini";


        // Clear the id's before grabbing new ones.
        id0.clear();
        id1.clear();
        id2.clear();
        id3.clear();
        id4.clear();
        id5.clear();
        id6.clear();
        id7.clear();
        id16.clear();
        id17.clear();
        id18.clear();
        id19.clear();
        id20.clear();
        id21.clear();
        id22.clear();
        id23.clear();

        // Clear the application lists before grabbing new ones.
        id0App.clear();
        id1App.clear();
        id2App.clear();
        id3App.clear();
        id4App.clear();
        id5App.clear();
        id6App.clear();
        id7App.clear();
        id16App.clear();
        id17App.clear();
        id18App.clear();
        id19App.clear();
        id20App.clear();
        id21App.clear();
        id22App.clear();
        id23App.clear();

        // Defaults
        int faderConfig = 99;

        String[] applicationArray = new String[0];
        Vector<String> appVector = new Vector<>();

        // Read config file
        try (BufferedReader br = new BufferedReader(new FileReader(configPath))) {
            String line;

            while ((line = br.readLine()) != null) {
                String applicationConfig;
                if (line.startsWith("[fader ")) {
                    faderConfig = Integer.parseInt(line.substring(7, line.length() - 1));
                } else if (line.startsWith("[knob ")) {
                    faderConfig = Integer.parseInt(line.substring(6, line.length() - 1));
                } else if (line.startsWith("application =")) {
                    applicationConfig = line.substring(14).trim();
                    applicationArray = applicationConfig.split(":");
                    appVector.addAll(Arrays.asList(applicationArray));
                    logger.info("Control {} bound to {}", faderConfig, appVector);
                    constructConfig(faderConfig, new Vector<>(appVector));
                    faderConfig = 99;
                    appVector.clear();
                }
            }
        } catch (IOException IOE) {
            logger.error("Error reading configs from file: {}", IOE.getMessage());
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Config refresh finished (took {}ms)", duration);

    }

    /**
     * Thread-safe entry point: uses configLock.
     */
    public void constructConfig(int fader, Vector<String> applications) {
        synchronized (configLock) {
            constructConfigUnsafe(fader, applications);
        }
    }

    /**
     * Internal implementation; caller must hold configLock.
     */
    private void constructConfigUnsafe(int fader, Vector<String> applications) {

        // Set application id's for faders.
        switch (fader) {
            case 0:
                this.id0App = applications;

                for (String app : id0App) {
                    logger.debug("Processing application: {} for fader {}", app, fader);
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        this.id0.addAll(temp_id);
                    }
                }
                break;

            case 1:
                this.id1App = applications;

                for (String app : id1App) {
                    logger.debug("Processing application: {} for fader {}", app, fader);
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        this.id1.addAll(temp_id);
                    }
                }
                break;

            case 2:
                this.id2App = applications;

                for (String app : id2App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        this.id2.addAll(temp_id);
                    }
                }
                break;

            case 3:
                this.id3App = applications;

                for (String app : id3App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        this.id3.addAll(temp_id);
                    }
                }
                break;

            case 4:
                this.id4App = applications;

                for (String app : id4App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        this.id4.addAll(temp_id);
                    }
                }
                break;

            case 5:
                this.id5App = applications;

                for (String app : id5App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        this.id5.addAll(temp_id);
                    }
                }
                break;

            case 6:
                this.id6App = applications;

                for (String app : id6App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        this.id6.addAll(temp_id);
                    }
                }
                break;

            case 7:
                this.id7App = applications;

                for (String app : id7App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        this.id7.addAll(temp_id);
                    }
                }
                break;

            case 16:
                this.id16App = applications;

                for (String app : id16App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        this.id16.addAll(temp_id);
                    }
                }
                break;

            case 17:
                this.id17App = applications;

                for (String app : id17App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        this.id17.addAll(temp_id);
                    }
                }
                break;

            case 18:
                this.id18App = applications;

                for (String app : id18App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        this.id18.addAll(temp_id);
                    }
                }
                break;

            case 19:
                this.id19App = applications;

                for (String app : id19App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        this.id19.addAll(temp_id);
                    }
                }
                break;

            case 20:
                this.id20App = applications;

                for (String app : id20App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        this.id20.addAll(temp_id);
                    }
                }
                break;

            case 21:
                this.id21App = applications;

                for (String app : id21App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        this.id21.addAll(temp_id);
                    }
                }
                break;

            case 22:
                this.id22App = applications;

                for (String app : id22App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        this.id22.addAll(temp_id);
                    }
                }
                break;

            case 23:
                this.id23App = applications;

                for (String app : id23App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        this.id23.addAll(temp_id);
                    }
                }
                break;

            default:
                break;
        }

    }
}
