package com.znsio.teswiz.tools.cmd;

import com.znsio.teswiz.exceptions.CommandLineExecutorException;
import com.znsio.teswiz.tools.OsUtils;
import com.znsio.teswiz.tools.SensitiveDataMasker;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommandLineExecutor {
    private static final Logger LOGGER = LogManager.getLogger(CommandLineExecutor.class.getName());
    private static final int DEFAULT_COMMAND_TIMEOUT = 60;
    private static final int TIMED_OUT_EXIT_CODE = 124; // common "timeout" convention

    private CommandLineExecutor() {}

    public static CommandLineResponse execCommand(final String[] command) {
        return execCommand(command, DEFAULT_COMMAND_TIMEOUT);
    }

    public static CommandLineResponse execCommand(final String[] command, int timeoutInSeconds) {
        String jointCommand = String.join(" ", command);
        return execCommandInternal(jointCommand, timeoutInSeconds, true);
    }

    /**
     * Executes a command without forcing a shell wrapper.
     * Prefer this when you don't need shell features like pipes/redirection.
     */
    public static CommandLineResponse execCommand(final List<String> command, int timeoutInSeconds) {
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("command cannot be null/empty");
        }
        // Build a nice display string, but execute without shell.
        String display = String.join(" ", command);
        return execCommandInternal(display, timeoutInSeconds, false, command);
    }

    /**
     * Best-effort check to see whether a command exists on PATH.
     * Works on Windows/Linux/macOS and in CI runners.
     */
    public static boolean isCommandAvailable(String tool) {
        if (tool == null || tool.trim().isEmpty()) return false;

        // 'where' on Windows, 'which' on Unix
        String trimmed = tool.trim();
        String[] cmd = OsUtils.isWindows()
                       ? new String[]{"where", trimmed}
                       : new String[]{"which", trimmed};

        try {
            CommandLineResponse r = execCommand(cmd, 5);
            return r.getExitCode() == 0 && r.getStdOut() != null && !r.getStdOut().trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Best-effort "tool works" check (covers cases where tool exists but isn't runnable).
     * Tries: <tool> --version then <tool> version
     */
    public static boolean toolWorks(String tool) {
        if (!isCommandAvailable(tool)) return false;

        try {
            CommandLineResponse r1 = execCommand(new String[]{tool, "--version"}, 5);
            if (r1.getExitCode() == 0) return true;

            CommandLineResponse r2 = execCommand(new String[]{tool, "version"}, 5);
            return r2.getExitCode() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static CommandLineResponse execCommandInternal(String jointCommand,
            int timeoutInSeconds,
            boolean useShell) {
        return execCommandInternal(jointCommand, timeoutInSeconds, useShell, null);
    }

    private static CommandLineResponse execCommandInternal(String displayCommand,
            int timeoutInSeconds,
            boolean useShell,
            List<String> directCommandParts) {
        String maskedDisplayCommand = SensitiveDataMasker.mask(displayCommand);
        String message = "\tExecuting Command: " + maskedDisplayCommand;
        long start = System.nanoTime();

        CommandLineResponse response = new CommandLineResponse();
        response.setCommand(displayCommand);

        Process process = null;
        try {
            process = createProcessBuilder(displayCommand, useShell, directCommandParts).start();
            recordExitStatus(process, timeoutInSeconds, response);

            // Read outputs (best effort). Even on timeout, streams may contain partial output.
            response.setStdOut(safeRead(process, true));
            response.setErrOut(safeRead(process, false));
            response.setDurationMillis(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));

            logCompletion(response);
            writeOutputArtifact(response, maskedDisplayCommand);
            return response;

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt(); // safe even if not interrupted
            throw new CommandLineExecutorException("Error " + message, e);
        } catch (RuntimeException e) {
            // Catch unexpected things like IllegalThreadStateException, etc.
            throw new CommandLineExecutorException("Error " + message + " :: " + e, e);
        } finally {
            closeStreams(process);
        }
    }

    private static ProcessBuilder createProcessBuilder(String displayCommand, boolean useShell,
            List<String> directCommandParts) {
        if (!useShell) {
            // Execute directly (no shell). Safer for quoting/args.
            return new ProcessBuilder(directCommandParts);
        }
        // Maintain legacy behavior: execute through shell to support pipes/redirects etc.
        return OsUtils.isWindows()
               ? new ProcessBuilder("cmd.exe", "/c", displayCommand)
               : new ProcessBuilder("sh", "-c", displayCommand);
    }

    private static void recordExitStatus(Process process, int timeoutInSeconds, CommandLineResponse response)
            throws InterruptedException {
        boolean finished = process.waitFor(timeoutInSeconds, TimeUnit.SECONDS);
        if (finished) {
            response.setExitCode(process.exitValue());
            return;
        }
        // IMPORTANT: avoid IllegalThreadStateException from exitValue()
        response.setTimedOut(true);
        response.setExitCode(TIMED_OUT_EXIT_CODE);
        killProcess(process);
    }

    // Kill process to free CI agents/VMs: ask politely first, then force.
    private static void killProcess(Process process) throws InterruptedException {
        process.destroy();
        process.waitFor(2, TimeUnit.SECONDS);
        if (process.isAlive()) {
            process.destroyForcibly();
        }
    }

    private static void logCompletion(CommandLineResponse response) {
        String responseMessage = String.format(
                "Command completed: exitCode=%d, timedOut=%s, durationMs=%d, command=\"%s\"",
                response.getExitCode(),
                response.isTimedOut(),
                response.getDurationMillis(),
                SensitiveDataMasker.mask(response.getCommand())
        );
        if (response.isTimedOut() || response.getExitCode() != 0) {
            LOGGER.warn(responseMessage);
        } else {
            LOGGER.info(responseMessage);
        }
    }

    private static void writeOutputArtifact(CommandLineResponse response, String maskedDisplayCommand) {
        if (response.getStdOut().isBlank() && response.getErrOut().isBlank()) {
            return;
        }
        java.nio.file.Path artifact = CommandOutputArtifactWriter.write(response.getCommand(), response.getStdOut(),
                response.getErrOut());
        if (artifact != null) {
            LOGGER.debug("Command output artifact: " + artifact);
        } else {
            LOGGER.warn("Unable to write command output artifact for command: " + maskedDisplayCommand);
        }
    }

    private static void closeStreams(Process process) {
        if (process == null) {
            return;
        }
        closeQuietly(process.getInputStream());
        closeQuietly(process.getErrorStream());
        closeQuietly(process.getOutputStream());
    }

    private static void closeQuietly(java.io.Closeable stream) {
        try {
            stream.close();
        } catch (Exception ignored) {
            // best effort: the process has finished or been killed
        }
    }

    private static String safeRead(Process process, boolean stdout) {
        if (process == null) return "";
        try {
            if (stdout) {
                return IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8).trim();
            }
            return IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "";
        }
    }

}
