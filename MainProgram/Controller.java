package MainProgram;

import Model.ClimateHandler;
import Model.Loghandler;
import View.ClimateDisplay;
import se.mau.DA343A.VT25.assignment2.*;
import se.mau.DA343A.VT25.assignment2.Buffer;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;


public class Controller implements IPlayPauseButtonPressedCallback {
    private final ClimateDisplay view;
    private final ClimateHandler climateHandler;
    private boolean isUpdating = false;
    Buffer<String> loggerQueue = new Buffer<>();
    Loghandler logger = new Loghandler(loggerQueue);

    public Controller() throws IOException, InterruptedException {
        BufferedImage mapImage = new ImageResources().getMapImage();
        BufferedImage maskImage = new ImageResources().getMapIsLandMaskImage();
        IIsLand island = new IsLandFromMaskImage(maskImage);

        view = new ClimateDisplay(mapImage, this);
        view.addPlayPauseButtonCallback(this);

        climateHandler = new ClimateHandler(island, this::processClimateUpdate, loggerQueue, logger);

        // Starta GUI:t i en separat tråd
//        javax.swing.SwingUtilities.invokeLater(() -> {
//            javax.swing.JFrame frame = new javax.swing.JFrame("Weather Application");
//            frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
//            frame.add(view);
//            frame.pack();
//            frame.setVisible(true);
//        });
    }


    private void processClimateUpdate(List<GridTemperature> temperatures) {
        view.refreshTemperatureDisplay(temperatures);
    }

    public void shutdown() {
        System.out.println("Closing ClimateHandler...");
        climateHandler.deactivateHandler();
    }


//    @Override
//    public void playPauseButtonPressed() {
//        if (!isUpdating) {
//            climateHandler.start();
//        } else {
//            climateHandler.stop();
//        }
//        isUpdating = !isUpdating;
//    }

    @Override
    public void playPauseButtonPressed() {
        boolean isUpdatingClimate = isUpdating;

        if (isUpdatingClimate) {
            System.out.println("[INFO] ❄️ Pausing climate updates...");
            isUpdating = false;
            climateHandler.stop();
        } else {
            System.out.println("[INFO] 🔄 Resuming climate updates...");
            isUpdating = true;
            climateHandler.start();
        }

        logClimateStatus(isUpdatingClimate);
    }

    private void logClimateStatus(boolean status) {
        String stateMessage = status ? "Updating resumed" : "Updating paused";
        System.out.println("[DEBUG] Climate update status: " + stateMessage);

        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get("climate_status_log.txt"),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            writer.write("[STATUS] " + LocalDateTime.now() + " - " + stateMessage);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("[ERROR] Could not write to climate_status_log.txt: " + e.getMessage());
        }
    }






}
