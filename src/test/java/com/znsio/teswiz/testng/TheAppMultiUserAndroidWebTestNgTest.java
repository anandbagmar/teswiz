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

        // AppBL.provideInvalidDetailsForSignup()/loginAgain() both return LoginBL, which
        // currently exposes no further methods - a genuine dead end, so there is nothing
        // to chain onto. Holding each persona's own AppBL instance and reusing it for
        // both of its calls (instead of constructing a fresh AppBL per call) is the
        // closest equivalent this BL API allows.
        AppBL personaI = new AppBL(PERSONA_I, Platform.android);
        AppBL personaYou = new AppBL(PERSONA_YOU, Platform.web);

        personaI.provideInvalidDetailsForSignup("znsio1", "invalid password");
        personaYou.provideInvalidDetailsForSignup("znsio2", "invalid password");

        personaI.loginAgain("znsio3", "invalid password");
        personaYou.loginAgain("znsio4", "invalid password");
    }

    private void createDriversForBothPersonas() {
        TestExecutionContext context = Runner.getTestExecutionContext(Thread.currentThread().getId());
        context.addTestState(PERSONA_I, "znsio1");
        Drivers.createDriverFor(PERSONA_I, Platform.android, context);
        context.addTestState(PERSONA_YOU, "znsio2");
        Drivers.createDriverFor(PERSONA_YOU, Platform.web, context);
    }
}
