package com.znsio.teswiz.assertions;

import com.znsio.teswiz.runner.CurrentStep;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.core.api.AssertionErrorCollector;

import java.util.ArrayList;
import java.util.List;

public class StepAttributingErrorCollector implements AssertionErrorCollector {

    private static final Logger LOGGER =
            LogManager.getLogger(StepAttributingErrorCollector.class.getName());

    private final List<AssertionError> collected = new ArrayList<>();
    private final long threadId;

    public StepAttributingErrorCollector(long threadId) {
        this.threadId = threadId;
    }

    @Override
    public void collectAssertionError(AssertionError error) {
        collected.add(label(error, CurrentStep.describe(threadId)));
    }

    @Override
    public List<AssertionError> assertionErrorsCollected() {
        return collected;
    }

    @Override
    public void succeeded() {
        // nothing to record on success
    }

    @Override
    public boolean wasSuccess() {
        return collected.isEmpty();
    }

    private AssertionError label(AssertionError original, String step) {
        if (step == null) {
            return original;
        }
        AssertionError labelled = new AssertionError("[" + step + "] " + original.getMessage(), original.getCause());
        labelled.setStackTrace(original.getStackTrace());
        return labelled;
    }
}
