package com.znsio.teswiz.web.playwright;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Drains a worker process's stderr on a daemon thread and keeps only the last few lines.
 *
 * <p>Draining matters on its own: an unread stderr pipe fills up and blocks the worker. Keeping the
 * tail lets a "worker closed unexpectedly" error say why, for example a missing npm package.
 */
final class WorkerStderrTail {
    private static final int MAX_LINES = 20;

    private final Deque<String> lines = new ArrayDeque<>();
    private final Thread drainer;

    private WorkerStderrTail(InputStream stderr) {
        drainer = new Thread(() -> drain(stderr), "playwright-worker-stderr");
        drainer.setDaemon(true);
    }

    static WorkerStderrTail start(InputStream stderr) {
        WorkerStderrTail tail = new WorkerStderrTail(stderr);
        tail.drainer.start();
        return tail;
    }

    /** Waits briefly for the stream to end (the worker exited) so its last words are captured. */
    String recentOutput(Duration maxWait) {
        try {
            drainer.join(maxWait.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        synchronized (lines) {
            return String.join(System.lineSeparator(), lines);
        }
    }

    private void drain(InputStream stderr) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stderr, StandardCharsets.UTF_8))) {
            String line;
            while (null != (line = reader.readLine())) {
                remember(line);
            }
        } catch (IOException ignored) {
            // The stream closes when the worker is shut down; nothing more to capture.
        }
    }

    private void remember(String line) {
        synchronized (lines) {
            if (lines.size() == MAX_LINES) {
                lines.removeFirst();
            }
            lines.addLast(line);
        }
    }
}
