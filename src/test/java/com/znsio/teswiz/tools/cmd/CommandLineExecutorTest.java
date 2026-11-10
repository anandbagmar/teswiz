package com.znsio.teswiz.tools.cmd;

import com.znsio.teswiz.exceptions.CommandLineExecutorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the observable behaviour of {@link CommandLineExecutor}: shell vs direct execution, captured
 * and trimmed output, exit codes, timeouts and failure handling. Runs real processes, so it uses
 * POSIX commands and is disabled on Windows.
 */
@DisabledOnOs(OS.WINDOWS)
class CommandLineExecutorTest {

    @Test
    void shellCommandCapturesTrimmedStdoutAndSucceeds() {
        CommandLineResponse response = CommandLineExecutor.execCommand(new String[]{"echo", "'  hello  '"});

        assertThat(response.getExitCode()).isZero();
        assertThat(response.isTimedOut()).isFalse();
        assertThat(response.getStdOut()).isEqualTo("hello");
        assertThat(response.getErrOut()).isEmpty();
        assertThat(response.getCommand()).isEqualTo("echo '  hello  '");
        assertThat(response.getDurationMillis()).isNotNegative();
    }

    @Test
    void shellCommandSupportsPipes() {
        CommandLineResponse response = CommandLineExecutor.execCommand(new String[]{"echo abc | tr a x"}, 10);

        assertThat(response.getStdOut()).isEqualTo("xbc");
    }

    @Test
    void failingCommandReportsExitCodeAndStderr() {
        CommandLineResponse response = CommandLineExecutor.execCommand(
                new String[]{"echo out; echo err 1>&2; exit 3"}, 10);

        assertThat(response.getExitCode()).isEqualTo(3);
        assertThat(response.isTimedOut()).isFalse();
        assertThat(response.getStdOut()).isEqualTo("out");
        assertThat(response.getErrOut()).isEqualTo("err");
    }

    @Test
    void commandExceedingTimeoutIsKilledAndMarkedTimedOut() {
        CommandLineResponse response = CommandLineExecutor.execCommand(new String[]{"sleep 20"}, 1);

        assertThat(response.isTimedOut()).isTrue();
        assertThat(response.getExitCode()).isEqualTo(124);
        assertThat(response.getDurationMillis()).isLessThan(15_000L);
    }

    @Test
    void listCommandRunsWithoutAShell() {
        CommandLineResponse response = CommandLineExecutor.execCommand(List.of("echo", "a | b"), 10);

        assertThat(response.getExitCode()).isZero();
        assertThat(response.getStdOut()).isEqualTo("a | b");
        assertThat(response.getCommand()).isEqualTo("echo a | b");
    }

    @Test
    void nullOrEmptyListCommandIsRejected() {
        assertThatThrownBy(() -> CommandLineExecutor.execCommand((List<String>) null, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CommandLineExecutor.execCommand(List.of(), 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missingExecutableInListFormFailsWithExecutorException() {
        assertThatThrownBy(() -> CommandLineExecutor.execCommand(List.of("teswiz-no-such-tool-xyz"), 5))
                .isInstanceOf(CommandLineExecutorException.class)
                .hasMessageContaining("teswiz-no-such-tool-xyz");
    }

    @Test
    void isCommandAvailableDetectsToolsOnPath() {
        assertThat(CommandLineExecutor.isCommandAvailable("sh")).isTrue();
        assertThat(CommandLineExecutor.isCommandAvailable("teswiz-no-such-tool-xyz")).isFalse();
        assertThat(CommandLineExecutor.isCommandAvailable(null)).isFalse();
        assertThat(CommandLineExecutor.isCommandAvailable("  ")).isFalse();
    }

    @Test
    void toolWorksForARunnableTool() {
        assertThat(CommandLineExecutor.toolWorks("java")).isTrue();
        assertThat(CommandLineExecutor.toolWorks("teswiz-no-such-tool-xyz")).isFalse();
    }
}
