package io.github.katacc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.sound.midi.MidiMessage;
import java.io.IOException;



public class AudioController {

    private static AudioController single_instance = null;

    public int id5;
    public int id6;
    public int id7;

    private int reScanId;


    String[] id7apps = {"Last Epoch.exe", "Warframe.x64.exe"};
    String id5App = "spotify";
    String id6App = "Zen";


    private AudioController() {
        this.id5 = 0;
        this.id6 = 0;
        this.id7 = 0;
        this.reScanId = 0;
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

        AudioController controller = AudioController.getInstance();

        if (reScanId >= 100) {
            controller.id5 = AudioController.getInstance().getId(id5App);
            controller.id6 = AudioController.getInstance().getId(id6App);
            for (String app : id7apps) {
                int temp_id = AudioController.getInstance().getId(app);
                if (temp_id != 0) {
                    controller.id7 = temp_id;
                }
            }
            reScanId = 0;
        }
        reScanId++;

        if (control == 5) {
            try {
                int id = this.id5;
                String command = String.format("wpctl set-volume %s, %s", id, scaled_volume);
                Process process = Runtime.getRuntime().exec(command);
            } catch (IOException e) {
                System.out.println("Erorr: " + e.getMessage());
            }
        }
        if (control == 6) {
            try {
                int id = this.id6;
                String command = String.format("wpctl set-volume %s, %s", id, scaled_volume);
                Process process = Runtime.getRuntime().exec(command);
            } catch (IOException e) {
                System.out.println("Erorr: " + e.getMessage());
            }
        }
        if (control == 7) {
            try {
                int id = this.id7;
                String command = String.format("wpctl set-volume %s, %s", id, scaled_volume);
                Process process = Runtime.getRuntime().exec(command);
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
    }

    /**
     * getId to get id of a application from pw-dump
     * Problem is that its really slow to generate the dump and parse the json
     * Therefore only get the updated id's every 100 midi messages.
     *
     * */
    public int getId(String name) {


        String targetName = name;
        int appId = 0;

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
                        appId = node.path("id").asInt();
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return appId;
    }
}
