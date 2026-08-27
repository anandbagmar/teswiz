package com.znsio.teswiz.testng;

import com.znsio.teswiz.businessLayer.theapp.AppBL;
import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.Platform;
import com.znsio.teswiz.runner.Drivers;
import com.znsio.teswiz.runner.Runner;
import org.testng.annotations.Test;

// Ported from theapp.feature's "Orchestrating multiple users on different platforms as
// part of same test (android-web)" scenario, calling the same AppBL unchanged.
public class TheAppMultiUserAndroidWebTestNgTest {
    private static final String PERSONA_I = "I";
    private static final String PERSONA_YOU = "You";

    @Test(groups = {"multiuser-android-web", "android", "web", "theapp"})
    public void orchestrateMultipleUsersOnDifferentPlatforms() {
        createDriversForBothPersonas();

        new AppBL(PERSONA_I, Platform.android).provideInvalidDetailsForSignup("znsio1", "invalid password");
        new AppBL(PERSONA_YOU, Platform.web).provideInvalidDetailsForSignup("znsio2", "invalid password");

        new AppBL(PERSONA_I, Runner.getPlatformForUser(PERSONA_I)).loginAgain("znsio3", "invalid password");
        new AppBL(PERSONA_YOU, Runner.getPlatformForUser(PERSONA_YOU)).loginAgain("znsio4", "invalid password");
    }

    private void createDriversForBothPersonas() {
        TestExecutionContext context = Runner.getTestExecutionContext(Thread.currentThread().getId());
        context.addTestState(PERSONA_I, "znsio1");
        Drivers.createDriverFor(PERSONA_I, Platform.android, context);
        context.addTestState(PERSONA_YOU, "znsio2");
        Drivers.createDriverFor(PERSONA_YOU, Platform.web, context);
    }
}
