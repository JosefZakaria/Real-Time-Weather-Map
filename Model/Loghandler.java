package Model;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import se.mau.DA343A.VT25.assignment2.Logger;
import se.mau.DA343A.VT25.assignment2.Buffer;


public class Loghandler extends Logger implements Runnable {
    private final BufferedWriter logWriter;
    private final Buffer<String> logQueue;
    private volatile boolean running = true;

    public Loghandler(Buffer<String> logQueue) throws IOException {
        this.logQueue = logQueue;
        logWriter = Files.newBufferedWriter(Paths.get("weather_log.txt"), StandardOpenOption.APPEND);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(this);

    }

    @Override
    protected void writeMessage(String message) {
        try {
            logWriter.write("[INFO] " + message + " at " + LocalDateTime.now());
            logWriter.newLine();
            logWriter.flush();
        } catch (IOException e) {
            System.out.println("Error while trying to write to file.");
        }
    }


    @Override
    public void run() {
        while (running) {
            try {
                String message = logQueue.get();

                if (message == null) {
                    Thread.sleep(100);
                    continue;
                }

                writeMessage(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("Error with logging messages" + e.getMessage());
            }
        }
    }


//    public void stop() {
//        running = false;
//        System.out.println("Loghandler stopped.");
//    }
}


