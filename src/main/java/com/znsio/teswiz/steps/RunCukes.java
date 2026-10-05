package com.znsio.teswiz.steps;

import com.znsio.teswiz.context.SessionContext;
import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.runner.CurrentStep;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeStep;
import io.cucumber.java.Scenario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RunCukes {
    private static final Logger LOGGER = LogManager.getLogger(RunCukes.class.getName());
    private final TestExecutionContext context;

    public RunCukes() {
        long threadId = Thread.currentThread().getId();
        LOGGER.info(String.format("RunCukes: ThreadId: '%s': Constructor", threadId));
        context = SessionContext.getTestExecutionContext(threadId);
    }

    @Before
    public void beforeScenario(Scenario scenario) {
        long threadId = Thread.currentThread().getId();
        LOGGER.info("RunCukes: ThreadId : '%d' :: beforeScenario: '%s'".formatted(threadId, scenario.getName()));
        new Hooks().beforeScenario(scenario);
    }

    @BeforeStep
    public void beforeStep(Scenario scenario) {
        long threadId = Thread.currentThread().getId();
        CurrentStep.advance(threadId);
    }

    @After
    public void afterScenario(Scenario scenario) {
        long threadId = Thread.currentThread().getId();
        LOGGER.info("RunCukes: ThreadId : '%d' :: afterScenario: '%s'".formatted(threadId, scenario.getName()));
        new Hooks().afterScenario(scenario);
    }
}
