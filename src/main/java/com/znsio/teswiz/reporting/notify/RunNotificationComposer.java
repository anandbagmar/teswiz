package com.znsio.teswiz.reporting.notify;

import java.util.List;

/**
 * The client-implemented seam that turns a finished run into zero or more ready-to-send {@link Notification}s. teswiz
 * owns the end-of-run trigger and delivery; the client owns what the message says. teswiz discovers the implementation
 * at the end of a run (from the {@code notify.composer.class} setting), calls {@link #compose(RunResult)} once the
 * cucumber report is complete, and delivers whatever is returned through the {@link Notifier}.
 * <p>
 * Implementations MUST have a public no-argument constructor so teswiz can instantiate them reflectively. Returning an
 * empty list means "nothing to send" (e.g. the client decided this run should not notify).
 */
public interface RunNotificationComposer {

    /**
     * Composes the notifications for a finished run.
     *
     * @param result the completed run's facts (report path, counts, environment, provenance)
     * @return the messages to deliver (possibly empty, never {@code null})
     */
    List<Notification> compose(RunResult result);
}
