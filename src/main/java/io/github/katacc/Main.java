package io.github.katacc;


public class Main {
    public static void main(String[] args) throws InterruptedException {
        MidiHandler.midiHandler();
        AudioController controller = AudioController.getInstance();



        controller.id5 = AudioController.getInstance().getId(controller.id5App);
        controller.id6 = AudioController.getInstance().getId(controller.id6App);
        for (String app : controller.id7apps) {
            int temp_id = AudioController.getInstance().getId(app);
            if (temp_id != 0) {
                controller.id7 = temp_id;
            }
        }


        /*
        * Commented out because of high CPU usage.
        * */
//        boolean run = true;

//        long lastExecution = System.nanoTime();
//        long lastSeconds = TimeUnit.NANOSECONDS.toSeconds(lastExecution);

//        long interval = 10;

//        while (run) {
//            long currentTime = System.nanoTime();
//            long currentSeconds = TimeUnit.NANOSECONDS.toSeconds(currentTime);

//            if (currentSeconds - lastSeconds >= interval) {

//                controller.id5 = AudioController.getInstance().getId(id5App);
//                controller.id6 = AudioController.getInstance().getId(id6App);
//                for (String app : id7apps) {
//                    int temp_id = AudioController.getInstance().getId(app);
//                    if (temp_id != 0) {
//                        controller.id7 = temp_id;
//                    }
//                }

//                lastExecution = System.nanoTime();
//                lastSeconds = TimeUnit.NANOSECONDS.toSeconds(lastExecution);


//            }
//        }
    }
}
