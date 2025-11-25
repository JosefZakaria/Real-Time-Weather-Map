package Model;

import se.mau.DA343A.VT25.assignment2.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class ClimateHandler implements Runnable {
    private final Weather weather;
    private Timer climateUpdater;
    private final Buffer<String> logPipeline;
    private final Consumer<List<GridTemperature>> temperatureCallback;
    private final List<Integer> activeSensorList = new ArrayList<>();
    private volatile boolean isUpdatingClimate = false;
    private volatile boolean sensorsInitialized = false;
    private volatile boolean getSensors = false;
    

    public ClimateHandler(IIsLand island, Consumer<List<GridTemperature>> temperatureCallback, Buffer<String> logPipeline, Loghandler logger) throws IOException {
        this.temperatureCallback = temperatureCallback;
        this.weather = new Weather(island);
        this.logPipeline = logPipeline;

        Thread weatherThread = new Thread(weather);
        weatherThread.start();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            retrieveSensorDetails();
            sensorsInitialized = true;
            initiateDataUpdate();
        } catch (IOException e) {
            System.err.println("Error when getting sensor" + e.getMessage());
        }
    }

    public void start() {
        isUpdatingClimate = true;
    }

    public void stop() {
        isUpdatingClimate = false;
    }

    public void deactivateHandler() {
        stop();
        try {
            weather.close();
            System.out.println("Weather turned of correct.");
        } catch (IOException e) {
            System.err.println("Error when turning of" + e.getMessage());
        }
    }

    private void initiateDataUpdate() {
        climateUpdater = new Timer();
        climateUpdater.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isUpdatingClimate && sensorsInitialized) {
                    gatherClimatedata();
                }
            }
        }, 0, 500); // Var 0,5 sekund
    }

    private void retrieveSensorDetails() throws IOException {
        if (getSensors) return;

        DataOutputStream out = new DataOutputStream(new BufferedOutputStream((OutputStream) weather.getOutput()));
        DataInputStream in = new DataInputStream(new BufferedInputStream((InputStream) weather.getInput()));

        out.writeInt(1);
        out.writeInt(1);
        out.flush();

        int version = in.readInt();
        int messageType = in.readInt();
        int numSensors = in.readInt();

        if (version != 1 || messageType != 2) {
            System.err.println("[ERROR] Unvalid answer from Weather");
            return;
        }

        for (int i = 0; i < numSensors; i++) {
            int sensorId = in.readInt();
            String sensorName = in.readUTF();
            activeSensorList.add(sensorId);
        }

        getSensors = true;
        sensorsInitialized = true;
    }



    private void gatherClimatedata() {
        try {
            DataOutput out = weather.getOutput();
            DataInput in = weather.getInput();
            List<GridTemperature> temperatures = new ArrayList<>();

            for (int sensorId : activeSensorList) {
                out.writeInt(1);
                out.writeInt(3);
                out.writeInt(sensorId);
                ((DataOutputStream) out).flush();

                int version = in.readInt();
                int messageType = in.readInt();
                int receivedSensorId = in.readInt();
                int row = in.readInt();
                int col = in.readInt();
                double temperature = in.readDouble();

                temperatures.add(new GridTemperature(row, col, temperature));


                String tempStatus = temperature < 0 ? "❄️ Cold" : temperature > 30 ? "🔥 Hot" : "Mild";
                System.out.printf("[INFO] Sensor %d at (%d, %d): %.2f°C - %s\n",
                        receivedSensorId, row, col, temperature, tempStatus);

                String logEntry = String.format("[%s] Temp: %.2f°C at (%d, %d) from Sensor %d",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        temperature, row, col, receivedSensorId);
                logPipeline.put(logEntry);
            }

            if (!temperatures.isEmpty()) {
                temperatureCallback.accept(temperatures);
            }
        } catch (IOException e) {
            System.err.println("Could not get temperature " + e.getMessage());
        }
    }



//    public int getState() {
//        return isUpdatingClimate ? 1 : 0;
//    }


}
