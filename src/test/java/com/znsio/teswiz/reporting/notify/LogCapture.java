package com.znsio.teswiz.reporting.notify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;

/**
 * Test helper: records every message a class logs while it is open, so a test can assert what an operator would see
 * in the log (and, just as important, what must never appear in it). Use it in a try-with-resources block.
 */
final class LogCapture implements AutoCloseable {

    private final Logger logger;
    private final CapturingAppender appender = new CapturingAppender();

    private LogCapture(Class<?> loggedClass) {
        this.logger = (Logger) LogManager.getLogger(loggedClass.getName());
        this.appender.start();
        this.logger.addAppender(appender);
    }

    /** @return a capture of the messages logged by {@code loggedClass} until it is closed */
    static LogCapture of(Class<?> loggedClass) {
        return new LogCapture(loggedClass);
    }

    /** @return every message captured so far, formatted, in the order logged */
    List<String> messages() {
        return appender.messages();
    }

    /** @return all captured messages joined by newlines, for a simple "contains" or "does not contain" check */
    String allMessages() {
        return String.join("\n", messages());
    }

    @Override
    public void close() {
        logger.removeAppender(appender);
        appender.stop();
    }

    private static final class CapturingAppender extends AbstractAppender {

        private final List<String> captured = Collections.synchronizedList(new ArrayList<>());

        private CapturingAppender() {
            super("LogCapture", null, null, false, null);
        }

        @Override
        public void append(LogEvent event) {
            captured.add(event.getMessage().getFormattedMessage());
        }

        private List<String> messages() {
            synchronized (captured) {
                return new ArrayList<>(captured);
            }
        }
    }
}
