package com.znsio.teswiz.tools;

import java.time.Duration;

import com.znsio.teswiz.network.FallbackHttpClient;
import com.znsio.teswiz.network.HttpRequest;

public class HeartBeat implements Runnable {
    // LogManager/Logger are fully-qualified ON PURPOSE (same as JUnitResults): an auto-import-organizer in this
    // environment rewrites a plain log4j import to java.util.logging.LogManager and drops Logger, breaking the build.
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager
            .getLogger(HeartBeat.class.getName());
    private static final int HTTP_OK = 200;
    private static final Duration CHECK_TIMEOUT = Duration.ofSeconds(30);
    private static final String PURPOSE = "heartbeat";
    private final String threadName;
    private final String apiUrl;
    private final int pollingDuration;
    private int heartBeatCounter = 0;
    private volatile boolean running = true; // Flag to control the loop
    private Thread currentThread;

    public HeartBeat(String userPersona, String url, int pollingDurationInSec) {
        this.threadName = userPersona;
        this.apiUrl = url;
        this.pollingDuration = pollingDurationInSec;
    }

    @Override
    public void run() {
        currentThread = Thread.currentThread();
        currentThread.setName(threadName);
        while (running) { // Continue running as long as the flag is true
            try {
                String message = "HeartBeat: '%s', API is '%s'. Current count: '%d'";
                String status = "up";
                if (!checkHeartBeat()) {
                    status = "down";
                }
                heartBeatCounter++;
                message = String.format(message, this.threadName, status, heartBeatCounter);
                ReportPortalLogger.logInfoMessage(message);
                Thread.sleep(this.pollingDuration * 1000); // Sleep for x seconds
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    /**
     * One check of the API, through teswiz's shared fallback HTTP client (a host with an unreachable address is
     * still checked through its other addresses). Redirects are followed, as before.
     *
     * @return true when the API answered 200
     */
    boolean checkHeartBeat() {
        try {
            HttpRequest request = HttpRequest.get(apiUrl, CHECK_TIMEOUT).followingRedirects();
            return new FallbackHttpClient(PURPOSE).send(request).status() == HTTP_OK;
        } catch (Exception e) {
            LOGGER.debug("HeartBeat check of '{}' failed: {}", threadName, e.getMessage());
            return false;
        }
    }

    // Method to fetch the number of heartbeat calls done
    public int getHeartBeatCounter() {
        return heartBeatCounter;
    }

    // Method to stop the heartbeat
    public void stopHeartBeat() {
        String message = String.format("HeartBeat: Stopping heartbeat for user: '%s' with url: '%s' after '%d' counts",
                this.threadName, this.apiUrl, heartBeatCounter);
        ReportPortalLogger.logInfoMessage(message);
        running = false;
        if (this.currentThread != null) {
            currentThread.interrupt(); // Interrupt the thread if it's sleeping
        }
    }
}
