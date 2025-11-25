package View;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import MainProgram.Controller;
import se.mau.DA343A.VT25.assignment2.GUI;
import se.mau.DA343A.VT25.assignment2.GridTemperature;
import se.mau.DA343A.VT25.assignment2.IPlayPauseButtonPressedCallback;


public class ClimateDisplay extends GUI {
    private Controller controller;
    private final List<IPlayPauseButtonPressedCallback> callbacks = new ArrayList<>();

    public ClimateDisplay(BufferedImage mapImage, Controller controller) {
        super(mapImage);
        this.controller = controller;
        super.startGUIOnNewThread();
    }

    @Override
    public void addPlayPauseButtonCallback(IPlayPauseButtonPressedCallback callback) {
        callbacks.add(callback);
    }

    @Override
    public void removePlayPauseButtonCallback(IPlayPauseButtonPressedCallback callback) {
        callbacks.remove(callback);
    }

    @Override
    protected void invokePlayPauseButtonCallbacks() {
        for (IPlayPauseButtonPressedCallback callback : callbacks) {
            callback.playPauseButtonPressed();
        }
    }

    @Override
    protected void onExiting() {
        controller.shutdown();
    }


    public void refreshTemperatureDisplay(List<GridTemperature> temperatureList) {
        temperatureList.forEach(temp -> {
            String tempStatus;
            if (temp.temperature() < 0) {
                tempStatus = "❄️ Freezing!";
            } else if (temp.temperature() > 25) {
                tempStatus = "🔥 Hot!";
            } else {
                tempStatus = "Mild";
            }

            System.out.printf("[INFO] Sensor at (%d,%d) reports: %.2f°C - %s\n",
                    temp.row(), temp.col(), temp.temperature(), tempStatus);
        });

        super.setTemperatures(temperatureList);
        repaint();
    }

}
