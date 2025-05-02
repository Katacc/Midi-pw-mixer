package io.github.katacc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;

import javax.sound.midi.MidiMessage;
import java.io.IOException;
import java.util.*;


public class AudioController {

    private static AudioController single_instance = null;

    private List<Integer> id0;
    private List<Integer> id1;
    private List<Integer> id2;
    private List<Integer> id3;
    private List<Integer> id4;
    private List<Integer> id5;
    private List<Integer> id6;
    private List<Integer> id7;

    private int reScanId;


    private Vector<String> id7App;
    private Vector<String> id5App;
    private Vector<String> id6App;
    private Vector<String> id4App;
    private Vector<String> id3App;
    private Vector<String> id2App;
    private Vector<String> id1App;
    private Vector<String> id0App;

    private AudioController() {

        id0 = new ArrayList<>();
        id1 = new ArrayList<>();
        id2 = new ArrayList<>();
        id3 = new ArrayList<>();
        id4 = new ArrayList<>();
        id5 = new ArrayList<>();
        id6 = new ArrayList<>();
        id7 = new ArrayList<>();

        this.reScanId = 0;

        id0App = new Vector<>();
        id1App = new Vector<>();
        id2App = new Vector<>();
        id3App = new Vector<>();
        id4App = new Vector<>();
        id5App = new Vector<>();
        id6App = new Vector<>();
        id7App = new Vector<>();
    }

    public static AudioController getInstance() {
        if (single_instance == null) {
            single_instance = new AudioController();
        }
        return single_instance;
    }

    public void changeVolume(MidiMessage msg) {
        
        byte[] message = msg.getMessage();

        byte control = message[1];
        byte value = message[2];

        float scaled_volume = (((float) value / 127) * 100) / 100;


        if (reScanId >= 200) {
            getConfig();
            reScanId = 0;
        }
        reScanId++;

        if (control == 0) {
            try {
                    String command = String.format("wpctl set-volume %s, %s", id0, scaled_volume);
                    Process process = Runtime.getRuntime().exec(command);
            } catch (IOException e) {
                System.out.println("Erorr: " + e.getMessage());
            }
        }
        if (control == 1) {
            try {
                String command = String.format("wpctl set-volume %s, %s", id1, scaled_volume);
                Process process = Runtime.getRuntime().exec(command);
            } catch (IOException e) {
                System.out.println("Erorr: " + e.getMessage());
            }
        }
        if (control == 2) {
            try {
                String command = String.format("wpctl set-volume %s, %s", id2, scaled_volume);
                Process process = Runtime.getRuntime().exec(command);
            } catch (IOException e) {
                System.out.println("Erorr: " + e.getMessage());
            }
        }
        if (control == 3) {
            try {
                String command = String.format("wpctl set-volume %s, %s", id3, scaled_volume);
                Process process = Runtime.getRuntime().exec(command);
            } catch (IOException e) {
                System.out.println("Erorr: " + e.getMessage());
            }
        }
        if (control == 4) {
            try {
                for (int id : id4) {
                    String command = String.format("wpctl set-volume %s, %s", id, scaled_volume);
                    Process process = Runtime.getRuntime().exec(command);
                }
            } catch (IOException e) {
                System.out.println("Erorr: " + e.getMessage());
            }
        }
        if (control == 5) {
            try {
                for (int id : id5) {
                    String command = String.format("wpctl set-volume %s, %s", id, scaled_volume);
                    Process process = Runtime.getRuntime().exec(command);
                }
            } catch (IOException e) {
                System.out.println("Erorr: " + e.getMessage());
            }
        }
        if (control == 6) {
            try {
                String command = String.format("wpctl set-volume %s, %s", id6, scaled_volume);
                Process process = Runtime.getRuntime().exec(command);
            } catch (IOException e) {
                System.out.println("Erorr: " + e.getMessage());
            }
        }
        if (control == 7) {
            try {
                for (int id : id7) {
                    String command = String.format("wpctl set-volume %s, %s", id, scaled_volume);
                    Process process = Runtime.getRuntime().exec(command);
                }
            } catch (IOException e) {
                System.out.println("Erorr: " + e.getMessage());
            }
        }
        if (control == 41) {
            if (value == 127) {
                try {
                    String command = String.format("playerctl play-pause");
                    Process process = Runtime.getRuntime().exec(command);
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
        if (control == 44) {
            if (value == 127) {
                try {
                    String command = String.format("playerctl next");
                    Process process = Runtime.getRuntime().exec(command);
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
        if (control == 43) {
            if (value == 127) {
                try {
                    String command = String.format("playerctl previous");
                    Process process = Runtime.getRuntime().exec(command);
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
        if (control == 42) {
            if (value == 127) {
                try {
                    String command = String.format("playerctl stop");
                    Process process = Runtime.getRuntime().exec(command);
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
        if (control == 46) {
            if (value == 127) {
                getConfig();
                System.out.println("Forced reconfig");
            }
        }
    }

    /**
     * getId to get id of a application from pw-dump
     * Problem is that its really slow to generate the dump and parse the json
     * Therefore only get the updated id's every 100 midi messages.
     *
     * */
    public List<Integer> getId(String name) {


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
            System.out.println("Error: " + e.getMessage());
        }

        return appId;
    }

    public void getConfig() {
        // Path to config file
        String userHome = System.getProperty("user.home");
        String configPath = userHome + "/.config/midi-mixer/config.ini";


        // Defaults
        int faderConfig = 99;

        String[] applicationArray = new String[0];
        Vector<String> appVector = new Vector<>();

        // Read config file
        var state = ReadingState.FADER;
        try (BufferedReader br = new BufferedReader(new FileReader(configPath))) {

            do {

                String applicationConfig = null;

                String line = br.readLine();

                if (line == null) {
                    state = ReadingState.DONE;
                }

                // Parse config file with state machine
                switch (state) {
                    case FADER:
                        faderConfig = Integer.parseInt(line.substring(7, 8));
                        state = state.nextState();
                        break;

                    case APPLICATION:
                        applicationConfig = line.substring(14);
                        applicationArray = applicationConfig.split(":");
                        appVector.addAll(Arrays.asList(applicationArray));
                        state = state.nextState();
                        break;

                    case BLANK:
                        System.out.println(faderConfig + " " + Arrays.toString(applicationArray));
                        constructConfig(faderConfig, appVector);
                        state = state.nextState();

                        faderConfig = 99;
                        appVector.clear();
                        break;

                    case DONE:
                        break;
                }

            } while (state != ReadingState.DONE);

        } catch (IOException IOE) {
            System.out.println("Error reading configs from file: " + IOE.getMessage());
        }
    }

    public void constructConfig(int fader, Vector<String> applications) {

        AudioController controller = AudioController.getInstance();

        // Set application id's for faders.
        switch (fader) {
            case 0:
                id0App = applications;

                for (String app : id0App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        controller.id0.addAll(temp_id);
                    }
                }
                break;

            case 1:
                id1App = applications;

                for (String app : id1App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        controller.id1.addAll(temp_id);
                    }
                }
                break;

            case 2:
                id2App = applications;

                for (String app : id2App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        controller.id2.addAll(temp_id);
                    }
                }
                break;

            case 3:
                id3App = applications;

                for (String app : id3App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        controller.id3.addAll(temp_id);
                    }
                }
                break;

            case 4:
                id4App = applications;

                for (String app : id4App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        controller.id4.addAll(temp_id);
                    }
                }
                break;

            case 5:
                id5App = applications;

                for (String app : id5App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        controller.id5.addAll(temp_id);
                    }
                }
                break;

            case 6:
                id6App = applications;

                for (String app : id6App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        controller.id6.addAll(temp_id);
                    }
                }
                break;

            case 7:
                id7App = applications;

                for (String app : id7App) {
                    List<Integer> temp_id = AudioController.getInstance().getId(app);
                    if (!temp_id.isEmpty()) {
                        controller.id7.addAll(temp_id);
                    }
                }
                break;

            default:
                break;
        }



    }
}
