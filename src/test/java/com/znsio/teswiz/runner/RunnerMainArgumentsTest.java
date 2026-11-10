package com.znsio.teswiz.runner;

import com.znsio.teswiz.exceptions.InvalidTestDataException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code Runner.main} is the entry point for {@code java -jar teswiz-<version>-all.jar}. Invalid
 * arguments must produce a clear usage error, whether or not the {@code LOG_DIR} system property
 * is set, instead of a {@link NullPointerException}.
 */
class RunnerMainArgumentsTest {
    private static final String LOG_DIR = "LOG_DIR";
    private static final String OUTPUT_DIRECTORY = "OUTPUT_DIRECTORY";

    private String originalLogDir;
    private String originalOutputDirectory;

    @BeforeEach
    void rememberSystemProperties() {
        originalLogDir = System.getProperty(LOG_DIR);
        originalOutputDirectory = System.getProperty(OUTPUT_DIRECTORY);
    }

    @AfterEach
    void restoreSystemProperties() {
        restore(LOG_DIR, originalLogDir);
        restore(OUTPUT_DIRECTORY, originalOutputDirectory);
    }

    @Test
    void noArgumentsWithoutLogDirFailsWithUsageInsteadOfNullPointer() {
        System.clearProperty(LOG_DIR);

        assertThatThrownBy(() -> Runner.main(new String[0]))
                .isInstanceOf(InvalidTestDataException.class)
                .hasMessageContaining("configFilePath")
                .hasMessageContaining("stepDefDirName")
                .hasMessageContaining("featuresDirName");
    }

    @Test
    void wrongArgumentCountWithLogDirFailsWithUsage() {
        System.setProperty(LOG_DIR, "target/runner-main-test");

        assertThatThrownBy(() -> Runner.main(new String[]{"only-one-argument"}))
                .isInstanceOf(InvalidTestDataException.class)
                .hasMessageContaining("configFilePath");
    }

    @Test
    void invalidArgumentsLeaveOutputDirectoryUntouched() {
        System.setProperty(OUTPUT_DIRECTORY, "existing-output-directory");
        System.setProperty(LOG_DIR, "target/runner-main-test");

        assertThatThrownBy(() -> Runner.main(new String[0])).isInstanceOf(InvalidTestDataException.class);

        assertThat(System.getProperty(OUTPUT_DIRECTORY)).isEqualTo("existing-output-directory");
    }

    private static void restore(String key, String value) {
        if (null == value) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
