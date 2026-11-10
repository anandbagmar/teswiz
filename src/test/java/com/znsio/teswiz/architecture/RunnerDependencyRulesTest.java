package com.znsio.teswiz.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Package-dependency rules for the teswiz framework (main sources only).
 *
 * <p>{@code com.znsio.teswiz.runner} is the top orchestration layer; the engine, config, mobile,
 * visual, API and reporting packages are libraries it calls. Those libraries still depend back on
 * {@code runner} today (mostly for configuration reads), which creates a package cycle. The frozen
 * rule records the existing back-references in {@code src/test/resources/archunit_store} and fails
 * only on new ones, so the cycle can only shrink.
 */
@AnalyzeClasses(packages = "com.znsio.teswiz", importOptions = ImportOption.DoNotIncludeTests.class)
class RunnerDependencyRulesTest {
    private static final String RUNNER = "com.znsio.teswiz.runner";

    // Layers that sit above or beside runner (the BDD glue and runtime hooks) may use it.
    private static final String[] ALLOWED_TO_USE_RUNNER = {
            RUNNER, "com.znsio.teswiz.steps..", "com.znsio.teswiz.testng..", "com.znsio.teswiz.screen..",
            "com.znsio.teswiz.listener..", "com.znsio.teswiz.businessLayer.."};

    @ArchTest
    static final ArchRule librariesDoNotGainNewDependenciesOnRunner = FreezingArchRule.freeze(
            noClasses().that().resideOutsideOfPackages(ALLOWED_TO_USE_RUNNER)
                    .should().dependOnClassesThat().resideInAPackage(RUNNER)
                    .because("runner orchestrates the libraries; a library depending on runner creates a cycle"));

    @ArchTest
    static final ArchRule mobileSessionStaysIndependentOfRunner = noClasses()
            .that().resideInAPackage("com.znsio.teswiz.mobile.session..")
            .should().dependOnClassesThat().resideInAPackage(RUNNER)
            .because("mobile.session is already free of runner dependencies and must stay that way");
}
